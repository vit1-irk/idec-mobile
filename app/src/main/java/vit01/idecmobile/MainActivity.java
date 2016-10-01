package vit01.idecmobile;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.SqliteTransport;
import vit01.idecmobile.Core.Station;

public class MainActivity extends AppCompatActivity {
    private static final int ADD_NODE = 100000;
    private static final int MANAGE_NODE = 100001;
    public Station currentStation;
    public ListView echoList;
    public ArrayAdapter echoListAdapter;
    public int currentStationIndex = 0;
    public Drawer drawer;
    public AccountHeader drawerHeader;
    public boolean is_offline_list_now = false;
    public SharedPreferences sharedPref;
    public SharedPreferences.Editor prefEditor;
    int MY_PERMISSION_WRITE_STORAGE;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Config.loadConfig(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_fetch);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Intent intent = new Intent(MainActivity.this, DebugActivity.class);
                intent.putExtra("task", "fetch");
                startActivity(intent);

                swipeRefresh.setRefreshing(false);
            }
        });

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        drawerHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withCompactStyle(true)
                .withProfileImagesVisible(false)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        if (profile instanceof IDrawerItem) {
                            long identifier = profile.getIdentifier();
                            int countStations = Config.values.stations.size();

                            if (identifier == ADD_NODE) {
                                Config.values.stations.add(new Station());
                                Intent editNew = new Intent(MainActivity.this, StationsActivity.class);
                                editNew.putExtra("index", countStations);
                                startActivity(editNew);
                            } else if (identifier == MANAGE_NODE) {
                                startActivity(new Intent(MainActivity.this, StationsActivity.class));
                            } else if (identifier < countStations) {
                                // Выбрали другую станцию, обновляем список
                                currentStationIndex = (int) identifier;
                                currentStation = Config.values.stations.get(currentStationIndex);
                                saveCurrentStationPosition(currentStationIndex);
                                updateEcholist();
                                return true;
                            }
                            return true;
                        }
                        return false;
                    }
                })
                .build();

        PrimaryDrawerItem echoItem = new PrimaryDrawerItem().withIdentifier(1).withName("Эхоконференции").withIcon(GoogleMaterial.Icon.gmd_message);
        PrimaryDrawerItem carbonItem = new PrimaryDrawerItem().withIdentifier(2).withName("Карбонка").withIcon(GoogleMaterial.Icon.gmd_input).withSelectable(false);
        PrimaryDrawerItem sentItem = new PrimaryDrawerItem().withIdentifier(3).withName("Отправленные").withIcon(GoogleMaterial.Icon.gmd_send).withSelectable(false);
        final PrimaryDrawerItem draftsItem = new PrimaryDrawerItem().withIdentifier(4).withName("Черновики").withIcon(GoogleMaterial.Icon.gmd_drafts).withSelectable(false);
        PrimaryDrawerItem starredItem = new PrimaryDrawerItem().withIdentifier(5).withName("Избранные").withIcon(GoogleMaterial.Icon.gmd_star).withSelectable(false);
        final PrimaryDrawerItem offlineItem = new PrimaryDrawerItem().withIdentifier(6).withName("Offline-эхи").withIcon(GoogleMaterial.Icon.gmd_signal_wifi_off);
        PrimaryDrawerItem extItem = new PrimaryDrawerItem().withIdentifier(7).withName("Дополнительно").withIcon(GoogleMaterial.Icon.gmd_extension).withSelectable(false);
        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem().withIdentifier(8).withName("Настройки").withIcon(GoogleMaterial.Icon.gmd_settings).withSelectable(false);
        PrimaryDrawerItem helpItem = new PrimaryDrawerItem().withIdentifier(9).withName("Помощь").withIcon(GoogleMaterial.Icon.gmd_help).withSelectable(false);
        PrimaryDrawerItem unreadItem = new PrimaryDrawerItem().withIdentifier(10).withName("Непрочитанные").withIcon(GoogleMaterial.Icon.gmd_remove_red_eye).withSelectable(false);

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(drawerHeader)
                .withToolbar(toolbar)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(false)
                .addDrawerItems(
                        echoItem,
                        carbonItem,
                        unreadItem,
                        sentItem,
                        draftsItem,
                        starredItem,
                        offlineItem,
                        extItem,
                        new DividerDrawerItem(),
                        settingsItem,
                        helpItem
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            long identifier = drawerItem.getIdentifier();

                            if (identifier == 8) {
                                startActivity(new Intent(MainActivity.this, CommonSettings.class));
                            } else if (identifier == 5) {
                                Intent intent = new Intent(MainActivity.this, EchoView.class);
                                intent.putExtra("echoarea", "_favorites");
                                intent.putExtra("nodeindex", -1);
                                startActivity(intent);
                            } else if (identifier == 1) {
                                is_offline_list_now = false;
                                updateEcholist();
                            } else if (identifier == 6) {
                                is_offline_list_now = true;
                                updateEcholist();
                            } else if (identifier == 9) {
                                startActivity(new Intent(MainActivity.this, HelpActivity.class));
                            } else if (identifier == 2) {
                                Intent intent = new Intent(MainActivity.this, EchoView.class);
                                intent.putExtra("echoarea", "_carbon_classic");
                                intent.putExtra("nodeindex", -1);
                                startActivity(intent);
                            } else if (identifier == 7) {
                                startActivity(new Intent(MainActivity.this, AdditionalActivity.class));
                            } else if (identifier == 3) {
                                Intent intent = new Intent(MainActivity.this, DraftsView.class);
                                intent.putExtra("unsent", false);
                                startActivity(intent);
                            } else if (identifier == 4) {
                                Intent intent = new Intent(MainActivity.this, DraftsView.class);
                                intent.putExtra("unsent", true);
                                startActivity(intent);
                            } else if (identifier == 10) {
                                AbstractTransport transport = new SqliteTransport(MainActivity.this);
                                ArrayList<String> unread = transport.getAllUnreadMessages();

                                if (unread.size() == 0) {
                                    Toast.makeText(MainActivity.this, "Непрочитанных сообщений нет!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Intent intent = new Intent(MainActivity.this, MessageSlideActivity.class);
                                    intent.putExtra("msglist", unread);
                                    intent.putExtra("position", 0);
                                    startActivity(intent);
                                }
                            }
                        }
                        return false;
                    }
                })
                .build();

        echoList = (ListView) findViewById(R.id.echolist);
        echoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String echoarea = ((TextView) view).getText().toString();
                Intent viewEcho = new Intent(MainActivity.this, EchoView.class);
                viewEcho.putExtra("echoarea", echoarea);

                if (is_offline_list_now) {
                    viewEcho.putExtra("nodeindex", -1);
                } else {
                    viewEcho.putExtra("nodeindex", currentStationIndex);
                }
                startActivity(viewEcho);
            }
        });

        echoList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                editEchoList();
                return true;
            }
        });

        updateStationsList();
        updateEcholist();

        if (Config.values.firstRun) {
            Config.values.firstRun = false;
            startActivity(new Intent(this, StationsActivity.class));
        }


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_WRITE_STORAGE);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        updateStationsList();
        updateEcholist();
        super.onResume();
    }

    public void updateStationsList() {
        // добавляем станции в navigation drawer

        drawerHeader.clear();

        for (int i = 0; i < Config.values.stations.size(); i++) {
            Station station = Config.values.stations.get(i);
            String nodename = station.nodename;

            drawerHeader.addProfile(new ProfileDrawerItem()
                    .withName(nodename)
                    .withIdentifier(i), i);
        }

        drawerHeader.addProfiles(
                new ProfileSettingDrawerItem().withName("Добавить станцию").withIdentifier(ADD_NODE).withIcon(GoogleMaterial.Icon.gmd_add),
                new ProfileSettingDrawerItem().withName("Управление станциями").withIdentifier(MANAGE_NODE).withIcon(GoogleMaterial.Icon.gmd_settings)
        );

        currentStationIndex = sharedPref.getInt("nodeindex_current", 0);

        if (currentStationIndex >= Config.values.stations.size()) {
            currentStationIndex = 0;
            saveCurrentStationPosition(0);
        }
        currentStation = Config.values.stations.get(currentStationIndex);
        drawerHeader.setActiveProfile(currentStationIndex);
    }

    public void updateEcholist() {
        if (is_offline_list_now) {
            echoListAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, Config.values.offlineEchoareas);
        } else {
            echoListAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, currentStation.echoareas);
        }
        echoList.setAdapter(echoListAdapter);
    }

    public void saveCurrentStationPosition(int position) {
        prefEditor = sharedPref.edit();
        prefEditor.putInt("nodeindex_current", position);
        prefEditor.apply();
    }

    public void editEchoList() {
        Intent intent = new Intent(MainActivity.this, ListEditActivity.class);

        if (is_offline_list_now) {
            intent.putExtra("type", "offline");
            intent.putExtra("index", -1);
        } else {
            intent.putExtra("type", "fromstation");
            intent.putExtra("index", currentStationIndex);
        }
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        menu.findItem(R.id.action_fetch).setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_get_app)
                        .actionBar().color(Color.WHITE));

        menu.findItem(R.id.action_send).setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_cloud_upload)
                        .actionBar().color(Color.WHITE));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, CommonSettings.class));
            return true;
        } else if (id == R.id.action_fetch) {
            Intent intent = new Intent(this, DebugActivity.class);
            intent.putExtra("task", "fetch");
            startActivity(intent);
            return true;
        } else if (id == R.id.action_send) {
            Intent intent = new Intent(this, DebugActivity.class);
            intent.putExtra("task", "send");
            startActivity(intent);
            return true;
        } else if (id == R.id.action_stations) {
            startActivity(new Intent(this, StationsActivity.class));
            return true;
        } else if (id == R.id.action_edit_echoareas) {
            editEchoList();
            return true;
        } else if (id == R.id.action_mark_all_base_read) {
            SqliteTransport transport = new SqliteTransport(this);
            transport.setUnread(false, (String) null);
            updateEcholist();
        }

        return super.onOptionsItemSelected(item);
    }
}
