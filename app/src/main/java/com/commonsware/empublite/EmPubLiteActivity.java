package com.commonsware.empublite;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import de.greenrobot.event.EventBus;

public class EmPubLiteActivity extends Activity implements FragmentManager.OnBackStackChangedListener {
    private ViewPager pager=null;
    private ContentsAdapter adapter=null;
    private static final String MODEL="model";
    private static final String PREF_LAST_POSITION="lastPosition";
    private ModelFragment mfrag=null;
    private static final String PREF_SAVE_LAST_POSITION="saveLastPosition";
    private static final String PREF_KEEP_SCREEN_ON="keepScreenOn";
    private View sidebar=null;
    private View divider=null;
    private SimpleContentFragment help=null;
    private SimpleContentFragment about=null;
    private static final String HELP="help";
    private static final String ABOUT="about";
    private static final String FILE_HELP=
            "file:///android_asset/misc/help.html";
    private static final String FILE_ABOUT=
            "file:///android_asset/misc/about.html";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStrictMode();
        setContentView(R.layout.main);
        sidebar=findViewById(R.id.sidebar);
        divider=findViewById(R.id.divider);
        getFragmentManager().addOnBackStackChangedListener(this);
        help=(SimpleContentFragment)getFragmentManager().findFragmentByTag(HELP);
        about=(SimpleContentFragment)getFragmentManager().findFragmentByTag(ABOUT);
        pager=(ViewPager)findViewById(R.id.pager);
        getActionBar().setHomeButtonEnabled(true);
        UpdateReceiver.scheduleAlarm(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        if (adapter==null) {
            mfrag=
                    (ModelFragment)getFragmentManager().findFragmentByTag(MODEL);
            if (mfrag == null) {
                mfrag=new ModelFragment();
                getFragmentManager().beginTransaction().add(mfrag, MODEL).commit();
            }
            else if (mfrag.getBook() != null) {
                setupPager(mfrag.getBook());
            }
        }
        if (mfrag.getPrefs() != null) {
            pager.setKeepScreenOn(mfrag.getPrefs()
                    .getBoolean(PREF_KEEP_SCREEN_ON, false));
        }
    }


    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        if (mfrag.getPrefs() != null) {
            int position=pager.getCurrentItem();
            mfrag.getPrefs().edit().putInt(PREF_LAST_POSITION, position)
                    .apply();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        return(super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                pager.setCurrentItem(0, false);
                return(true);
            case R.id.about:
                showAbout();
                return(true);
            case R.id.notes:
                Intent i=new Intent(this, NoteActivity.class);
                i.putExtra(NoteActivity.EXTRA_POSITION, pager.getCurrentItem());
                startActivity(i);
                return(true);
            case R.id.help:
                showHelp();
                return(true);
            case R.id.settings:
                startActivity(new Intent(this, Preferences.class));
                return(true);
            case R.id.update:
                WakefulIntentService.sendWakefulWork(this, DownloadCheckService.class);
                return(true);

        }
        return(super.onOptionsItemSelected(item));
    }



    private void setupPager(BookContents contents) {
        adapter=new ContentsAdapter(this, contents);
        pager.setAdapter(adapter);
        findViewById(R.id.progressBar1).setVisibility(View.GONE);
        findViewById(R.id.pager).setVisibility(View.VISIBLE);
        SharedPreferences prefs=mfrag.getPrefs();
        if (prefs != null) {
            if (prefs.getBoolean(PREF_SAVE_LAST_POSITION, false)) {
                pager.setCurrentItem(prefs.getInt(PREF_LAST_POSITION, 0));
            }
            pager.setKeepScreenOn(prefs.getBoolean(PREF_KEEP_SCREEN_ON, false));
        }
    }

    public void onEventMainThread(BookLoadedEvent event) {
        setupPager(event.getBook());
    }

    private void setupStrictMode() {
        StrictMode.ThreadPolicy.Builder builder=
                new StrictMode.ThreadPolicy.Builder().detectNetwork();
        if (BuildConfig.DEBUG) {
            builder.penaltyDeath();
        }
        else {
            builder.penaltyLog();
        }
        StrictMode.setThreadPolicy(builder.build());
    }
    private void openSidebar() {
        LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)sidebar.getLayoutParams();
        if (p.weight == 0) {
            p.weight=3;
            sidebar.setLayoutParams(p);
        }
        divider.setVisibility(View.VISIBLE);
    }

    private void showAbout() {
        if (sidebar != null) {
            openSidebar();
            if (about == null) {
                about=SimpleContentFragment.newInstance(FILE_ABOUT);
            }
            getFragmentManager().beginTransaction().addToBackStack(null)
                    .replace(R.id.sidebar, about).commit();
        }
        else {
            Intent i=new Intent(this, SimpleContentActivity.class);
            i.putExtra(SimpleContentActivity.EXTRA_FILE, FILE_ABOUT);
            startActivity(i);
        }
    }
    private void showHelp() {
        if (sidebar != null) {
            openSidebar();
            if (help == null) {
                help=SimpleContentFragment.newInstance(FILE_HELP);
            }
            getFragmentManager().beginTransaction().addToBackStack(null)
                    .replace(R.id.sidebar, help).commit();
        }
        else {
            Intent i=new Intent(this, SimpleContentActivity.class);
            i.putExtra(SimpleContentActivity.EXTRA_FILE, FILE_HELP);
            startActivity(i);
        }
    }
    @Override
    public void onBackStackChanged() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            LinearLayout.LayoutParams p=
                    (LinearLayout.LayoutParams)sidebar.getLayoutParams();
            if (p.weight > 0) {
                p.weight=0;
                sidebar.setLayoutParams(p);
                divider.setVisibility(View.GONE);
            }
        }
    }

}
