package storage.david.com.sidebyside2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import in.goodiebag.carouselpicker.CarouselPicker;

public class Photo extends AppCompatActivity implements View.OnTouchListener{
    ImageView photo;
    private final int PICK_PHOTO = 1;
    CarouselPicker carouselPicker;
    String dataArray[];
    Uri uri;
    BluetoothSPP spp;

    //DRAGGING & ZOOMING
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    //set of touch parameters for photo1
    private int mode = NONE;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private PointF start = new PointF();//PointF holds 2 coordinates
    private PointF mid = new PointF();
    private Bitmap bmap;
    private float oldDist = 1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo);

        //If these 2 lines are not added, the toolbar will appear without icon
        android.support.v7.widget.Toolbar myToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        //The scroll wheel
        carouselPicker = (CarouselPicker) findViewById(R.id.carousel);
        List<CarouselPicker.PickerItem> textItems = new ArrayList<>();
        //20 here represents the textSize in dp, change it to the value you want.
        textItems.add(new CarouselPicker.TextItem("days", 20));
        textItems.add(new CarouselPicker.TextItem("pounds", 20));
        textItems.add(new CarouselPicker.TextItem("inches", 20));
        textItems.add(new CarouselPicker.TextItem("BMIs", 20));
        CarouselPicker.CarouselViewAdapter textAdapter = new CarouselPicker.CarouselViewAdapter(this, textItems, 0);
        textAdapter.setTextColor(Color.MAGENTA);
        carouselPicker.setAdapter(textAdapter);

        photo=(ImageView)findViewById(R.id.photo);
        photo.setOnTouchListener(this);

    }

    /**
     * To make the toolbar appear
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo, menu);
        return true;
    }
    /**
     * What happen when a button in toolbar is clicked
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.selectPhoto:
                Intent pickPhoto1 = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto1, PICK_PHOTO);
                break;
            case R.id.seeComment:
                if(uri!=null) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    String message = "Note: ";
                    for (int j = 3; j < dataArray.length; j++) {
                        message += dataArray[j] + " ";
                    }
                    alertDialogBuilder.setMessage(message);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }else
                    Toast.makeText(this,"Select a photo first to see note.",Toast.LENGTH_SHORT).show();
                break;
            case R.id.editExif:
                if (uri != null) {
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
                                Intent intent=new Intent(Photo.this,DeviceList.class);
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
                                        File file = new File(getUriRealPath(Photo.this, uri));
                                        ExifInterface exifInterface = new ExifInterface(file.getCanonicalPath());
                                        exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description);
                                        exifInterface.saveAttributes();
                                        Toast.makeText(Photo.this, "Data successfully saved!", Toast.LENGTH_SHORT).show();
                                        updateCarousel();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(Photo.this, "Oops, an error prevents saving: "+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    }
                                }//onClick()
                            });
                    exifDialog.create().show();
                } else
                    Toast.makeText(this, "Select a photo first to see note.", Toast.LENGTH_SHORT).show();
        }//switch
        return true;
    }
    /**
     * Handle after user picks the photo
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //display the 1st select photo, and its data
        if (requestCode == PICK_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {

            uri = data.getData();
            //Display photo
            try {
                Bitmap bitmap1 = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                photo.setImageBitmap(bitmap1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            //read photo exif data
            updateCarousel();
        }
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                spp.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                spp.setupService();
            } else {
                Toast.makeText(this
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }//OnActivityResult() ends

    /**
     * Every time the data is updated, call this method to show the roller with new data
     */
    @TargetApi(24)
    public void updateCarousel(){
        InputStream in;
        String dataString = "Not found";
        try {
            in = getContentResolver().openInputStream(uri);
            ExifInterface exifInterface = new ExifInterface(in);
            dataString = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //this if else structure is to prevent empty String[] for photos w/o data
        if (dataString != null) {
            dataArray = dataString.split("\\s+");
        } else {
            dataString = "0 0 0 0";
            dataArray = dataString.split("\\s+");
        }
        if(dataArray.length>=4) {
            try {
                List<CarouselPicker.PickerItem> textItems = new ArrayList<>();
                textItems.add(new CarouselPicker.TextItem((String) getDate(uri), 15));
                textItems.add(new CarouselPicker.TextItem(dataArray[0] + "lbs", 15));
                textItems.add(new CarouselPicker.TextItem(dataArray[1] + "ins", 15));
                textItems.add(new CarouselPicker.TextItem(dataArray[2] + "BMIs", 15));
                CarouselPicker.CarouselViewAdapter textAdapter = new CarouselPicker.CarouselViewAdapter(this, textItems, 0);
                textAdapter.setTextColor(Color.MAGENTA);
                carouselPicker.setAdapter(textAdapter);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }else
            Toast.makeText(this,"Wrong # of parameters",Toast.LENGTH_SHORT).show();
    }
    /**
     * For getting the date taken of photo with that uri
     *
     * @param photoUri photo uri passed from imageview button handler
     * @return string MM/dd/yyyy
     */
    public CharSequence getDate(Uri photoUri) {
        Long longDate = null;
        String[] projection = new String[]{MediaStore.Images.Media.DATE_TAKEN};
        Cursor cur = managedQuery(photoUri, projection, null, null, null);
        if (cur.moveToFirst()) {//when cursor is empty
            int dateColumn = cur.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
            longDate = cur.getLong(dateColumn);
        }
        Date d = new Date(longDate);
        java.text.DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        return formatter.format(d);
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
    /**
     * find the distance between two fingers
     *
     * @param event
     * @return
     */
    private float findDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getX(1);
        float d = x * x + y * y;
        return (float) Math.sqrt(d);
    }

    /**
     * find the midpt between two fingers, used for Matrix.postScale()
     *
     * @param point
     * @param event
     */
    private void findMidPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * Dragging and Zooming using imageview matrix scaleType
     *
     * @param v     which imageview
     * @param event
     * @return
     */
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN://first pointer down
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = findDistance(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    findMidPoint(mid, event);
                    mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                    break;
                } else if (mode == ZOOM) {
                    float newDist = findDistance(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = (newDist / oldDist);
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
        }//switch
        photo.setImageMatrix(matrix);
        bmap = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas1 = new Canvas(bmap);
        photo.draw(canvas1);

        return true;
    }//onTouch() ends
}
