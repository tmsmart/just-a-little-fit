package ecap.studio.group.justalittlefit.activity;

import android.content.Context;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ecap.studio.group.justalittlefit.R;
import ecap.studio.group.justalittlefit.adapter.WorkoutRvAdapter;
import ecap.studio.group.justalittlefit.adapter.WorkoutRvNameAdapter;
import ecap.studio.group.justalittlefit.dialog.InformationDialog;
import ecap.studio.group.justalittlefit.model.Workout;
import ecap.studio.group.justalittlefit.util.Constants;
import ecap.studio.group.justalittlefit.util.Utils;

public class TodayChooserActivity extends BaseNaviDrawerActivity {

    private static final String DATE_FORMAT = "MMMM d, yyyy";

    @InjectView(R.id.tvDate)
    TextView tvDate;
    @InjectView(R.id.rvWorkoutName)
    RecyclerView rvWorkoutName;
    WorkoutRvNameAdapter workoutRvNameAdapter;
    List<Workout> workouts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_today_chooser, null, false);
        frameLayout.addView(contentView, 0);
        ButterKnife.inject(this, frameLayout);
        workouts = getWorkouts();
        setTitle(R.string.today_title_string);
        setDisplayDate();
        setupRecyclerView();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_info:
                displayInfoDialog();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    void setupDrawerContent(NavigationView navigationView) {
        // Check menu item of currently displayed activity
        MenuItem selectedItem = navigationView.getMenu().findItem(R.id.navi_today);
        selectedItem.setChecked(true);
        super.setupDrawerContent(navigationView);
    }

    private List<Workout> getWorkouts() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(Constants.WORKOUTS)) {
            return extras.getParcelableArrayList(Constants.WORKOUTS);
        } else {
            return null;
        }
    }

    private void setDisplayDate() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DATE_FORMAT);
        if (workouts.get(0) != null && workouts.get(0).getWorkoutDate() != null) {
            tvDate.setText(dateTimeFormatter.print(workouts.get(0).getWorkoutDate()));
        }
    }

    private void displayInfoDialog() {
        FragmentManager fm = getSupportFragmentManager();
        InformationDialog dialog = InformationDialog.newInstance(Constants.CHOOSER);
        dialog.show(fm, getString(R.string.infoDialogTagTodayChooser));
    }

    private void setupRecyclerView() {
        rvWorkoutName.setLayoutManager(new LinearLayoutManager(this));
        rvWorkoutName.setItemAnimator(new DefaultItemAnimator());

        workoutRvNameAdapter = new WorkoutRvNameAdapter(
                new ArrayList<>(workouts), this);
        rvWorkoutName.setAdapter(workoutRvNameAdapter);
    }
}
