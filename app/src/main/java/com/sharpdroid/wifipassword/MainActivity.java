package com.sharpdroid.wifipassword;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    List<WifiPw> WifiPwList = new ArrayList<>();
    List<String> WifiNameList = new ArrayList<>();
    List<String> WifiPwListst = new ArrayList<>();
    static Handler m_handler;
    static Runnable m_handlerTask;
    MaterialDialog wait;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        final RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(MainActivity.this);
        rv.setLayoutManager(llm);


        if (!RootShell.isRootAvailable()) {

            MaterialDialog show = new MaterialDialog.Builder(this).title(getString(R.string.noroottx)).iconRes(R.drawable.norooticon)
                    .theme(Theme.LIGHT).content(getString(R.string.notrootmsg)).positiveText(getString(R.string.oknorootmsg)).neutralText(getString(R.string.uninstall))
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent(Intent.ACTION_DELETE);
                            intent.setData(Uri.parse("package:com.sharpdroid.wifipassword"));
                            startActivity(intent);
                        }
                    })
                    .show();

        } else {
            if (RootShell.isAccessGiven()) {

                wait = new MaterialDialog.Builder(this).title(getString(R.string.wait))
                        .theme(Theme.LIGHT).content(getString(R.string.getpassword))
                        .progress(true, 0)
                        .show();

                GetWifiPW2();


            } else {
                WifiPwList = new ArrayList<>();
                WifiPwList.add(new WifiPw(getResources().getString(R.string.rootaccden), getResources().getString(R.string.rootperm)));
                RVAdapter adapter = new RVAdapter(WifiPwList);
                rv.setAdapter(adapter);
            }
        }
    }

    private void ErrorDialog() {
        final MaterialDialog error = new MaterialDialog.Builder(this).title(getString(R.string.nowififoundtitle))
                .theme(Theme.LIGHT).content(getString(R.string.nowififounddes)).positiveText(getString(R.string.oknorootmsg))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                })
                .show();
    }


    private void GetWifiPW() {
        RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(MainActivity.this);
        rv.setLayoutManager(llm);
        WifiPwList = new ArrayList<>();

        Command commandpw = new Command(0, "su", "cat /data/misc/wifi/wpa_supplicant.conf") {
            boolean ReadWifiName = false;
            boolean ReadWifiPw = false;
            String[] wifi = new String[2];


            @Override
            public void commandOutput(int id, String line) {

                if (!line.contains("No such file or directory")) {

                    if (ReadWifiPw) {
                        try {
                            if (line.trim().equals("key_mgmt=NONE")) {
                                wifi[1] = getResources().getString(R.string.nopw);
                            } else {
                                wifi[1] = line.replace("psk=", "");
                                wifi[1] = wifi[1].replaceAll("\"", "");
                                wifi[1] = wifi[1].trim();
                            }

                        } catch (Exception e) {
                            wifi[1] = line;
                        } finally {
                            ReadWifiName = false;
                            ReadWifiPw = false;
                            WifiPwList.add(new WifiPw(wifi[0], wifi[1]));
                            WifiPwListst.add(wifi[1]);
                        }
                    }

                    if (ReadWifiName) {
                        try {
                            wifi[0] = line.replace("ssid=", "");
                            wifi[0] = wifi[0].replaceAll("\"", "");
                            wifi[0] = wifi[0].trim();
                        } catch (Exception e) {
                            wifi[0] = line;
                        } finally {
                            ReadWifiName = false;
                            ReadWifiPw = true;
                            WifiNameList.add(wifi[0]);
                        }
                    }

                    if (line.contains("network={")) {
                        ReadWifiName = true;
                    }
                } else {
                    ErrorDialog();
                }

                super.commandOutput(id, line);
            }

            @Override
            public void commandTerminated(int id, String reason) {

                Log.v("Wifi Password", "Command terminated: " + reason);

            }

            @Override
            public void commandCompleted(int id, int exitcode) {
                Log.v("Wifi Password", "Command completed: " + exitcode);
                if (WifiPwList.isEmpty()) {
                   ErrorDialog();
                } else {
                    wait.dismiss();
                }
            }

        };
        try {
            RootShell.getShell(true).add(commandpw);
        } catch (Exception e) {
            WifiPwList.add(new WifiPw("Errore: ", e.toString()));
        }
        RVAdapter adapter = new RVAdapter(WifiPwList);
        rv.setAdapter(adapter);
        try {
            RootShell.closeAllShells();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void GetWifiPW2() {
        RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(MainActivity.this);
        rv.setLayoutManager(llm);
        WifiPwList = new ArrayList<>();
        try {
            boolean ReadWifiName = false;
            boolean ReadWifiPw = false;
            String[] wifi = new String[2];
            Process process = Runtime.getRuntime().exec("su -c cat /data/misc/wifi/wpa_supplicant.conf");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                //log.append(line + "\n");
                if (!line.contains("No such file or directory")) {

                    if (ReadWifiPw) {
                        try {
                            if (line.trim().equals("key_mgmt=NONE")) {
                                wifi[1] = getResources().getString(R.string.nopw);
                            } else {
                                wifi[1] = line.replace("psk=", "");
                                wifi[1] = wifi[1].replaceAll("\"", "");
                                wifi[1] = wifi[1].trim();
                            }
                        } catch (Exception e) {
                            wifi[1] = line;
                        } finally {
                            ReadWifiName = false;
                            ReadWifiPw = false;
                            WifiPwList.add(new WifiPw(wifi[0], wifi[1]));
                            WifiPwListst.add(wifi[1]);
                        }
                    }

                    if (ReadWifiName) {
                        try {
                            wifi[0] = line.replace("ssid=", "");
                            wifi[0] = wifi[0].replaceAll("\"", "");
                            wifi[0] = wifi[0].trim();
                        } catch (Exception e) {
                            wifi[0] = line;
                        } finally {
                            ReadWifiName = false;
                            ReadWifiPw = true;
                            WifiNameList.add(wifi[0]);
                        }
                    }

                    if (line.contains("network={")) {
                        ReadWifiName = true;
                    }

                } else {
                    wait.dismiss();
                    ErrorDialog();
                }
            }

            if (WifiPwList.isEmpty()) {
                wait.dismiss();
                ErrorDialog();
            } else {
                wait.dismiss();
            }

            //Log.v("WifiPassword", "Output: " + log);
        } catch (Exception e) {
            WifiPwList.add(new WifiPw("Errore", e.toString()));
        }

        RVAdapter adapter = new RVAdapter(WifiPwList);
        rv.setAdapter(adapter);

    }

    class WifiPw {
        String name;
        String pw;

        WifiPw(String name, String pw) {
            this.name = name;
            this.pw = pw;
        }
    }


    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.PersonViewHolder> {
        List<WifiPw> WifiPwList;

        public class PersonViewHolder extends RecyclerView.ViewHolder {
            CardView cv;
            TextView WifiName;
            TextView WifiPw;

            PersonViewHolder(View itemView) {
                super(itemView);
                cv = (CardView) itemView.findViewById(R.id.cv);
                WifiName = (TextView) itemView.findViewById(R.id.wifiname);
                WifiPw = (TextView) itemView.findViewById(R.id.wifipw);
            }
        }

        RVAdapter(List<WifiPw> WifiPwList) {
            this.WifiPwList = WifiPwList;
        }


        @Override
        public PersonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
            v.setOnClickListener(new OnRVClick());
            v.setOnLongClickListener(new OnRVLongClick());
            PersonViewHolder pvh = new PersonViewHolder(v);
            return pvh;
        }


        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

        @Override
        public void onBindViewHolder(PersonViewHolder personViewHolder, int i) {
            personViewHolder.WifiName.setText(WifiPwList.get(i).name);
            personViewHolder.WifiPw.setText(WifiPwList.get(i).pw);
        }

        @Override
        public int getItemCount() {
            return WifiPwList.size();
        }
    }

    class OnRVClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
            int itemPosition = rv.getChildPosition(v);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getResources().getString(R.string.password), WifiPwListst.get(itemPosition));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), R.string.pwcopied, Toast.LENGTH_LONG).show();
        }


    }

    class OnRVLongClick implements View.OnLongClickListener {

        @Override
        public boolean onLongClick(View v) {
            RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
            int itemPosition = rv.getChildPosition(v);

            String wifidatatx = getResources().getString(R.string.ssid) + ": " + WifiNameList.get(itemPosition) +
                    "\n" + getResources().getString(R.string.password) + ": " + WifiPwListst.get(itemPosition);
            Intent sendWifi = new Intent();
            sendWifi.setAction(Intent.ACTION_SEND);
            sendWifi.putExtra(Intent.EXTRA_TEXT, wifidatatx);
            sendWifi.setType("text/plain");
            startActivity(Intent.createChooser(sendWifi, getResources().getText(R.string.sharewifi)));
            return false;
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
