package storage.david.com.sidebyside2;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class Setting extends AppCompatActivity implements View.OnClickListener,CompoundButton.OnCheckedChangeListener{
    //for permanently storing data
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    Button btnTimePicker;
    TextView txtTime;
    private int mHour,mMinute;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //initialize permanent storage
        pref=getApplicationContext().getSharedPreferences("MyPref",MODE_PRIVATE);
        editor=pref.edit();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        //Button to launch clock interface and TextView to see selected time
        btnTimePicker = (Button) findViewById(R.id.btn_time);
        txtTime=(TextView)findViewById(R.id.in_time);
        btnTimePicker.setOnClickListener(this);

        //get user-selected hr and min saved when user last selected a time
        int userHour=pref.getInt("Hour",0);//get stored value for the particular key, if not, return 2nd value
        int userMin=pref.getInt("Minute",0);

        txtTime.setText(userHour + ":" +userMin);

        //switch for turning off and on the schedule notification
        Switch s=(Switch)findViewById(R.id.SwitchID);
        s.setOnCheckedChangeListener(this);
        boolean currentSwitchState=pref.getBoolean("switchState",true);
        s.setChecked(currentSwitchState);
    }

    //For Time Picker Dialog
    @Override
    public void onClick(View v){
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);

        //Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener(){

                    @Override
                    @TargetApi(16)
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute){
                        //permanently store user-input time
                        editor.putInt("Hour",hourOfDay);
                        editor.putInt("Minute",minute);
                        editor.apply();
                        txtTime.setText(hourOfDay + ":" +minute);//user selected hour and minute

                        //sending the pendingIntent to alarmManager for it execute notification
                        scheduleNotification(hourOfDay,minute);
                    }
                },mHour,mMinute,false);
        timePickerDialog.show();

    }
    //Building notification object
    @TargetApi(21)
    private Notification getNotification() {
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        //building a notification
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Take a photo & compare!");
        builder.setContentText("A little step step everyday amounts to something big!!!");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setColor(Color.MAGENTA);
        builder.setWhen(calendar.getTimeInMillis());
        builder.setShowWhen(true);
        builder.setSound(alarmSound);

        //so when the notification is pressed, user is re-directed to the app
        Intent notificationIntent=new Intent(this,MainActivity.class);
        PendingIntent contentIntent=PendingIntent.getActivity(this,0,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        return builder.build();
    }
    //send notification object to alarmManager (which can send the impending Intent when the app's asleep)
    private void scheduleNotification(int hourOfDay,int minute) {

        Intent notificationIntent = new Intent(this, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, getNotification());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager =  (AlarmManager) getSystemService(ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    //Switch on/off listener
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){

        Toast.makeText(this,"The Switch is "+(isChecked ? "on":"off"),Toast.LENGTH_SHORT).show();
        if(isChecked){

        }else{
            AlarmManager alarmManager =  (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent notificationIntent = new Intent(this, NotificationPublisher.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.cancel(pendingIntent);
        }
        editor.putBoolean("switchState",isChecked).apply();
    }


}
