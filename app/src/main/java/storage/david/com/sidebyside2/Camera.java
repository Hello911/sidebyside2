package storage.david.com.sidebyside2;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class Camera extends AppCompatActivity implements View.OnClickListener{
    WebView stream;
    ImageView shutter, gallery, reload, edit;
    Bitmap viewBitmap;//snapshot of the webview
    File file;//last snapshot taken
    BluetoothSPP spp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        stream=(WebView)findViewById(R.id.webView);
        stream.setWebViewClient(new MyBrowser());
        stream.getSettings().setJavaScriptEnabled(true);
        stream.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        stream.loadUrl("http://10.255.7.130:8081");
        stream.getSettings().setLoadWithOverviewMode(true);
        stream.getSettings().setUseWideViewPort(true);


        shutter=(ImageView) findViewById(R.id.shutter);
        shutter.setOnClickListener(this);

        gallery=(ImageView)findViewById(R.id.gallery);
        gallery.setOnClickListener(this);

        reload=(ImageView)findViewById(R.id.reload);
        reload.setOnClickListener(this);

        edit=(ImageView)findViewById(R.id.edit);
        edit.setOnClickListener(this);

        makeSSH();

    }
    public void makeSSH(){
        String user="pi";
        String password="raspberry123";
        String host="http://10.255.7.130";
        int port=22;
        try{
            JSch jsch=new JSch();
            Session session=jsch.getSession(user,host,port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking","no");
            session.setTimeout(10000);
            session.connect();
            ChannelExec channel=(ChannelExec)session.openChannel("exec");
            channel.setCommand("sudo service motion restart");
            channel.connect();
            channel.disconnect();
            Toast.makeText(this,"SSH successfully connected!",Toast.LENGTH_SHORT).show();
        }catch(JSchException e){
            e.printStackTrace();
            Toast.makeText(this,"SSH NOT connected: "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }
    @Override
     public void onClick(View v){
        switch(v.getId()){
            case R.id.edit:
                if(viewBitmap!=null){
                    LayoutInflater factory = LayoutInflater.from(this);
                    final View view = factory.inflate(R.layout.editdata, null);
                    final EditText strength= (EditText) view.findViewById(R.id.strength);
                    final EditText weight = (EditText) view.findViewById(R.id.weight);
                    final EditText height = (EditText) view.findViewById(R.id.height);
                    final EditText BMI = (EditText) view.findViewById(R.id.BMI);
                    final EditText note = (EditText) view.findViewById(R.id.note);
                    final Button bt = (Button) view.findViewById(R.id.bt);

                    //set up Bluetooth
                    spp=new BluetoothSPP(this);
                    //Toast user when BT is not available. And exit device selection window.
                    if(!spp.isBluetoothAvailable()){
                        Toast.makeText(this, "Bluetooth is NOT available.",Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    //when status is changed
                    spp.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
                        @Override
                        public void onDeviceConnected(String name, String address) {
                            strength.setText("Connected to "+name);
                        }
                        @Override
                        public void onDeviceDisconnected() {
                            strength.setText("Disconnected");
                        }

                        @Override
                        public void onDeviceConnectionFailed() {
                            strength.setText("Connection failed");
                        }
                    });
                    //listen to the Bluetooth button in dialog
                    bt.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick (View v){
                            if(spp.getServiceState()== BluetoothState.STATE_CONNECTED){
                                spp.disconnect();
                            }
                            else{
                                Intent intent=new Intent(Camera.this,DeviceList.class);
                                startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                            }
                        }
                    });
                    //To receive data
                    spp.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
                        public void onDataReceived(byte[] data, String message) {
                            strength.setText(message);
                        }
                    });
                    if (!spp.isBluetoothEnabled()) {//if Bluetooth is NOT enabled
                        //Enable Bluetooth
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, 1);
                        //Now check if Bluetooth is enabled
                        if (spp.isBluetoothEnabled()==true) {//if Bluetooth is now enabled
                            Toast.makeText(getApplicationContext(), "Bluetooth is ON", Toast.LENGTH_SHORT).show();
                        }
                        else {//if Bluetooth is still NOT enabled
                            Toast.makeText(getApplicationContext(), "Bluetooth OFF", Toast.LENGTH_SHORT).show();
                        }
                    } else {//if Bluetooth is already enabled
                        if (!spp.isServiceAvailable()) {//is spp service is NOT available
                            spp.setupService();
                            spp.startService(BluetoothState.DEVICE_OTHER);
                        }
                    }
                    //Bluetooth set up ENDS
                    AlertDialog.Builder exifDialog = new AlertDialog.Builder(this)
                            .setTitle("Edit Health Data")
                            .setView(view)
                            .setPositiveButton("save data", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if(weight.getText().toString().equals("")){
                                        weight.setText("0");
                                    }
                                    if(height.getText().toString().equals("")){
                                        height.setText("0");
                                    }
                                    if(BMI.getText().toString().equals("")){
                                        BMI.setText("0");
                                    }
                                    if("".equals(note.getText().toString())){
                                        note.setText("nothing written");
                                    }
                                    String description = weight.getText() + " "
                                            + height.getText() + " "
                                            + BMI.getText() + " "
                                            + note.getText();
                                    try {
                                        ExifInterface exifInterface = new ExifInterface(file.getCanonicalPath());
                                        exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description);
                                        exifInterface.saveAttributes();
                                        Toast.makeText(Camera.this, "Data successfully saved!", Toast.LENGTH_SHORT).show();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(Camera.this, "Oops, an error prevents saving.", Toast.LENGTH_SHORT).show();
                                    }
                                }//onClick()
                            });
                    exifDialog.create().show();
                }else
                    Toast.makeText(this, "Take a photo first",Toast.LENGTH_SHORT).show();
                break;
            case R.id.reload:
                stream.loadUrl("http://10.255.7.130:8081");
                makeSSH();
                Toast.makeText(this, "Reconnecting to the camera", Toast.LENGTH_SHORT).show();
                break;
            case R.id.shutter:
                try {
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat sdformat = new SimpleDateFormat("MM_dd_yyyy_HH:mm:ss");
                    String DateString = sdformat.format(calendar.getTime());

                    viewBitmap = getBitmapOfView(stream);
                    file = new File(getPublicDir(), "mySnapshot_" + DateString + ".png");
                    FileOutputStream fos = new FileOutputStream(file);
                    viewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    MediaScannerConnection.scanFile(Camera.this,
                            new String[]{file.getPath()},
                            null,
                            null);

                    Toast.makeText(this, "Snapshot taken", Toast.LENGTH_SHORT).show();
                }catch(IOException e){//for FileOutputStream
                    e.printStackTrace();
                    Toast.makeText(this, "Snapshot NOT stored.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.gallery:
                if(viewBitmap!=null){
                    ImageView image=new ImageView(this);
                    image.setImageBitmap(viewBitmap);
                    AlertDialog.Builder collageDialog=new AlertDialog.Builder(this)
                            .setView(image)
                            .setTitle("Last photo taken")
                            .setPositiveButton("share", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Generate a calendar String
                                    Calendar calendar=Calendar.getInstance();
                                    SimpleDateFormat sdformat=new SimpleDateFormat("MM_dd_yyyy_HH:mm:ss");
                                    String DateString=sdformat.format(calendar.getTime());

                                    //get Photo uri
                                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                                    viewBitmap.compress(Bitmap.CompressFormat.JPEG,100,bytes);
                                    //Force photo to scan for new photos stored.
                                    String path= MediaStore.Images.Media.insertImage(Camera.this.getContentResolver(),
                                            viewBitmap,
                                            "mySnapshot_"+DateString+".png",
                                            null);
                                    //share photo intent
                                    Uri uri=Uri.parse(path);
                                    //start sharing Intent
                                    Intent intent=new Intent(Intent.ACTION_SEND)
                                            .setType("image/*")
                                            .putExtra(Intent.EXTRA_STREAM,uri);
                                    try{
                                        startActivity(Intent.createChooser(intent, "Share last photo taken:"));
                                    }catch(ActivityNotFoundException e){
                                        e.printStackTrace();
                                    }

                                }//onClick()
                            });//DialogBuilder ENDS
                    collageDialog.create().show();
                }else
                    Toast.makeText(this, "Take a photo first",Toast.LENGTH_SHORT).show();
                break;
        }
     }

    /**
     * Allows you to open link in the browser.
     */
    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    /**
     * Get the Bitmap screenshot of a view
     * @param v
     * @return
     */
    public static Bitmap getBitmapOfView(View v){
        Bitmap b=Bitmap.createBitmap(v.getWidth(),
                v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);
        v.layout(v.getLeft(),v.getTop(),v.getRight(),v.getBottom());
        v.draw(c);
        return b;
    }
    /**
     *Get folder path
     * @return file path to a folder (not a file)
     */
    public File getPublicDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DCIM),"raspberry");
        if (!file.mkdirs()) {
            Log.e("PUBLIC DIRECTORY", "Directory not created");
        }
        return file;
    }
    /**
     * content:// style uri for photos canNOT be used to make change back to the photo
     * Use this method to get the REAL uri to make changes to the photo
     * @param context
     * @param uri
     * @return
     */
    private String getUriRealPath(Context context, Uri uri){
        String real="";
        if(isAboveKitKat()){//from KitKat above, the uri returned is not real path uri
            //sdk 19 (KitKat) or above
            real=getUriRealPathAboveKitkat(context, uri);
        }else{
            //below sdk 19
            real=getImageRealPath(getContentResolver(), uri, null);
        }
        return real;
    }//getUriRealPath() ends
    @TargetApi(19)
    private String getUriRealPathAboveKitkat(Context context, Uri uri){
        String real="";
        if(isGooglePhotoDoc(uri.getAuthority() )){//check if uri has authority "com.google.android.apps.photos.content"
            real=uri.getLastPathSegment();//gets the decoded last segment
        }else{
            real=getImageRealPath(getContentResolver(),uri,null);//ContentResolver provides access to content model
        }
        return real;
    }//getUriRealPathAboveKitkat() ends
    /**
     * Check if current android is above Kitkat sdk 19
     */
    private boolean isAboveKitKat(){
        boolean real=false;
        real= Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT;
        return real;
    }
    /**
     * Check if this document is provided by google photos
     */
    private boolean isGooglePhotoDoc(String uriAuthority){
        boolean ret=false;
        if("com.google.android.apps.photos.content".equals(uriAuthority)){
            ret=true;
        }
        return ret;
    }
    /**
     * For sdk lower than 19(KitKat)
     * Return uri that represent document file real local path
     */
    private String getImageRealPath(ContentResolver contentResolver, Uri uri, String whereClause){
        String ret="";
        //Query the uri with condition
        Cursor cursor=contentResolver.query(uri,null,whereClause,null,null);
        if(cursor!=null){
            boolean moveToFirst=cursor.moveToFirst();
            if(moveToFirst){
                //Get column name by uri type
                String columnName=MediaStore.Images.Media.DATA;
                if(uri==MediaStore.Images.Media.EXTERNAL_CONTENT_URI){
                    columnName=MediaStore.Images.Media.DATA;
                }else if(uri==MediaStore.Video.Media.EXTERNAL_CONTENT_URI){
                    columnName=MediaStore.Video.Media.DATA;
                }
                //get column index
                int imageColumnIndex=cursor.getColumnIndex(columnName);
                //get column value which is the uri related to local file path
                ret=cursor.getString(imageColumnIndex);

            }
        }
        return ret;
    }
}
