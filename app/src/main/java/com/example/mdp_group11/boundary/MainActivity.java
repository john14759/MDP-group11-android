package com.example.mdp_group11.boundary;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.mdp_group11.R;
import com.example.mdp_group11.adapter.AdapterResponse;
import com.example.mdp_group11.adapter.AdapterRobotStatus;
import com.example.mdp_group11.control.GridControl;
import com.example.mdp_group11.control.ResponseControl;
import com.example.mdp_group11.control.RobotStatusControl;
import com.example.mdp_group11.databinding.ActivityMainBinding;
import com.example.mdp_group11.enums.FaceDirection;
import com.example.mdp_group11.enums.SettingEnvironment;
import com.example.mdp_group11.test.BluetoothConnectionService;
import com.example.mdp_group11.test.BluetoothPopUp;
import com.example.mdp_group11.utils.SpacingItemDecoration;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    public static String TAG = "MainActivity_TAG";
    private static Context context;
    BluetoothConnectionService mBluetoothConnection;
    private boolean isTimerRunning = false;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private TextView textTimer;
    private Handler handler = new Handler();
    private Runnable runnable;
    private static MainActivity instance;
    private final int[] degreeDirection = {0, 180, 90, -90};
    private long startTimeMillis = 0;
    private int obstacle_count = 0;
    int milliseconds;
    int seconds;
    int minutes;
    BluetoothDevice mBTDevice;
    private static UUID myUUID;

    ObstacleBoxView obstacleBoxView;
    ObstacleView[] obstacleViews;
    RobotView robotView;

    public static MainActivity getInstance() {
        return instance;
    }

    int configOrient;
    String orientStr = "a";

    GridView gridMap;
    GridControl gridControl;

    double robotXcoord;

    double robotYcoord;

    EditText typeBoxEditText;
    SettingEnvironment settingEnvironment = SettingEnvironment.NULL;
    AdapterResponse adapterResponse;
    ResponseControl responseControl;
    AdapterRobotStatus adapterRobotStatus;
    RobotStatusControl robotStatusControl;
    BluetoothConnectionService bluetoothConnect;
    ImageButton forward;
    ImageButton backward;
    ImageButton turnLeft;
    ImageButton turnRight;
    ImageButton turnBackLeft;
    ImageButton turnBackRight;
    ImageButton buttonSendMessage;

    String rotationDetails = "0";

    Button buttonSendExploration;
    Button buttonSendFastest;
    Button ButtonReset;
    Button ButtonUndo;
    Button buttonStart;
    Button buttonStop;
    LinearLayout buttonHolder;
    TextView messageReceivedTextView;
    TextView messageReceived;
    ProgressDialog myDialog;
    TextView robotStatusTextView;

    Stack<String> actionStack = new Stack<String>();
    boolean isClickedExploration = false;
    boolean isClickedFastest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        configOrient = getResources().getConfiguration().orientation;
        if (configOrient == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main);
            orientStr = "p";
        }
        LocalBroadcastManager.getInstance(this);

        checkPermissions();
        instance = this;

        //Setting up shared preferences
        MainActivity.context = getApplicationContext();
        sharedPreferences();
        editor.putString("connStatus", "Disconnected");
        editor.commit();

        initializeGrid();
        initializeButtons();
        initializeMap();
        inializeRecycleViews();

        messageReceived=findViewById(R.id.comms_box);
        messageReceived.setMovementMethod(new ScrollingMovementMethod());

        Button bluetoothButton = (Button) findViewById(R.id.bluetoothButton);
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent popup = new Intent(MainActivity.this, BluetoothPopUp.class);
                startActivity(popup);
            }
        });

        myDialog = new ProgressDialog(new ContextThemeWrapper(MainActivity.this, R.style.MyDialogTheme));
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        if (!BluetoothConnectionService.isConnected()) {
            robotStatusTextView.setText("Waiting Bluetooth");
            robotStatusTextView.setTextColor(Color.BLACK);
        }

        else {
            Intent intent1 = getIntent();
            String str = intent1.getStringExtra("message_key");
            robotStatusTextView.setText(str);
        }

    }

    public void updateCommsBox(String message) {
        ScrollView scrollView = findViewById(R.id.scroll_view);
        if (messageReceivedTextView != null) {
            if (messageReceivedTextView.getText().length() > 0) {
                messageReceivedTextView.append("\n");
            }
            messageReceivedTextView.append(message);

            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }
    public void sendMovement(String t, Context context) {
        String toSend = "STM|" + t;
        Log.d("BLUETOOTH", "Sending up with letter " + t);
        try {
            BluetoothConnectionService.write(toSend.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Toast.makeText(context, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendData(String t, Context context) {
        try {
            BluetoothConnectionService.write(t.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Toast.makeText(context, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
        }
    }

    public void pressButton(int x) {
        ImageButton forward = findViewById(R.id.button_forward);
        ImageButton backward = findViewById(R.id.button_backward);
        ImageButton turnLeft = findViewById(R.id.button_turn_left);
        ImageButton turnRight = findViewById(R.id.button_turn_right);
        ImageButton turnBackLeft = findViewById(R.id.button_back_left);
        ImageButton turnBackRight = findViewById(R.id.button_back_right);
        switch (x) {
            case 1:
                forward.performClick();
                break;
            case 2:
                backward.performClick();
                break;
            case 3:
                turnLeft.performClick();
                break;
            case 4:
                turnRight.performClick();
                break;
            case 5:
                turnBackLeft.performClick();
                break;
            case 6:
                turnBackRight.performClick();
                break;
            default:
                break;
        }
    }

    private void initializeButtons() {

        forward = findViewById(R.id.button_forward);
        backward = findViewById(R.id.button_backward);
        turnLeft = findViewById(R.id.button_turn_left);
        turnRight = findViewById(R.id.button_turn_right);
        turnBackLeft = findViewById(R.id.button_back_left);
        turnBackRight = findViewById(R.id.button_back_right);
        robotStatusTextView = findViewById(R.id.robotStatus);

        ButtonReset = findViewById(R.id.button_reset);
        ButtonUndo = findViewById(R.id.button_undo);
        buttonHolder = findViewById(R.id.ll_button_holder);
        buttonSendExploration = findViewById(R.id.button_explore);
        buttonSendFastest = findViewById(R.id.button_fastest);
        buttonSendMessage = findViewById(R.id.send_message_button);
        buttonStart = findViewById(R.id.button_start);
        buttonStop = findViewById(R.id.button_stop);

        typeBoxEditText = findViewById(R.id.comms_edit_box);
        messageReceivedTextView = findViewById(R.id.comms_box);
        messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());
        ScrollView scrollView = findViewById(R.id.scroll_view);

        textTimer = findViewById(R.id.text_timer);
        actionStack = new Stack<String>();


        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isTimerRunning) {
                    startTimeMillis = System.currentTimeMillis();
                    isTimerRunning = true;
                    startTimer();
                }
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTimer();
            }
        });

        // reset timer on long click
        buttonStop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                resetTimer();
                return true;
            }
        });

        ButtonUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (obstacle_count > 0) {
                    obstacleViews[obstacle_count - 1].fullReset(orientStr);
                    obstacle_count--;
                }
            }
        });

        ButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gridControl.fullReset();
                for (ObstacleView obstacleView : obstacleViews) {
                    obstacleView.fullReset(orientStr);
                    Log.e(TAG, String.valueOf(obstacleView.getX()));
                    Log.e(TAG, String.valueOf(obstacleView.getY()));
                }
                robotView.fullReset();
                gridMap.invalidate();
                gridMap.printMap();
                obstacle_count = 0;

                String dataToSend = "BOARD RESET";
                if (BluetoothConnectionService.isConnected()) {
                    sendData(dataToSend, MainActivity.this);
                    if (messageReceivedTextView.getText().length() > 0) {
                        messageReceivedTextView.append("\n");
                    }
                    messageReceivedTextView.append(dataToSend);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
                }
            }
        });


        buttonSendExploration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button v = (Button) view;
                if (!isClickedExploration) {
                    v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_700)));
                    v.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    v.setTextColor(Color.WHITE);
                    buttonSendFastest.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    buttonSendFastest.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
                    buttonSendFastest.setTextColor(Color.BLACK);
                    isClickedExploration = true;
                    isClickedFastest = false;
                    robotStatusTextView.setText("Exploration Started");
                    robotStatusTextView.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.dark_green)));
                    if (!isTimerRunning) {
                        startTimeMillis = System.currentTimeMillis();
                        isTimerRunning = true;
                        startTimer();
                    }

                    List<Map<String, Object>> obstacleDetailsList = new ArrayList<>();

                    for (ObstacleView obstacleView : obstacleViews) {
                        if(obstacleView.getGridX() != -1 ) {
                            Map<String, Object> obstacleDetails = new HashMap<>();
                            obstacleDetails.put("x", obstacleView.getGridX());
                            obstacleDetails.put("y", obstacleView.getGridY());
                            obstacleDetails.put("d", obstacleView.getImageFace().toInt());
                            obstacleDetails.put("id", obstacleView.getObstacleId());
                            obstacleDetailsList.add(obstacleDetails);
                        }
                    }

                    robotXcoord = robotView.getGridX();
                    robotYcoord = robotView.getGridY();


                    List<Map<String, Object>> algoDetailsList = new ArrayList<>();
                    Map<String, Object> algoDetails = new HashMap<>();
                    algoDetails.put("obstacles", obstacleDetailsList);
                    algoDetails.put("robot_x", (robotXcoord-1));
                    algoDetails.put("robot_y", (robotYcoord-3));
                    algoDetails.put("robot_dir", rotationDetails);
                    algoDetails.put("retrying", "False");
                    algoDetailsList.add(algoDetails);


                    sendToAlgo(algoDetailsList);

                } else {
                    v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    v.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
                    v.setTextColor(Color.BLACK);
                    isClickedExploration = false;
                    robotStatusTextView.setText("Exploration Stopped");
                    robotStatusTextView.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.dark_red)));
                    stopTimer();
                }
            }
        });

        buttonSendFastest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button v = (Button) view;
                if (!isClickedFastest) {
                    v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_700)));
                    v.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    v.setTextColor(Color.WHITE);
                    buttonSendExploration.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    buttonSendExploration.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
                    buttonSendExploration.setTextColor(Color.BLACK);
                    isClickedFastest = true;
                    isClickedExploration = false;
                    robotStatusTextView.setText("Fastest Started");
                    robotStatusTextView.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.dark_green)));
                    if (!isTimerRunning) {
                        startTimeMillis = System.currentTimeMillis();
                        isTimerRunning = true;
                        startTimer();
                    }
                } else {
                    v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    v.setCompoundDrawableTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
                    v.setTextColor(Color.BLACK);
                    isClickedFastest = false;
                    robotStatusTextView.setText("Fastest Stopped");
                    robotStatusTextView.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.dark_red)));
                    stopTimer();
                }
            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tmp = "w";
                sendMovement(tmp, view.getContext());
                gridMap.invalidate();

                if (!robotView.getIsMoving()) {
                    robotView.addMovement("FF");
                }
                gridMap.printMap();

                String dataToSend = "STM|w";
                if (BluetoothConnectionService.isConnected()) {
                    if (messageReceivedTextView.getText().length() > 0) {
                        messageReceivedTextView.append("\n");
                    }
                    messageReceivedTextView.append(dataToSend);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });

        backward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tmp = "s";
                sendMovement(tmp, view.getContext());
                gridMap.invalidate();

                if (!robotView.getIsMoving()) {
                    robotView.addMovement("BB");
                }
                gridMap.printMap();

                String dataToSend = "STM|s";
                if (BluetoothConnectionService.isConnected()) {
                    if (messageReceivedTextView.getText().length() > 0) {
                        messageReceivedTextView.append("\n");
                    }
                    messageReceivedTextView.append(dataToSend);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });

        turnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tmp = "q";
                sendMovement(tmp, view.getContext());
                gridMap.invalidate();
                if (!robotView.getIsMoving()) {
                    robotView.addMovement("FL");
                }
                gridMap.printMap();

                String dataToSend = "STM|q";
                if (BluetoothConnectionService.isConnected()) {
                    if (messageReceivedTextView.getText().length() > 0) {
                        messageReceivedTextView.append("\n");
                    }
                    messageReceivedTextView.append(dataToSend);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });

        turnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tmp = "e";
                sendMovement(tmp, view.getContext());
                gridMap.invalidate();

                if (!robotView.getIsMoving()) {
                    robotView.addMovement("FR");
                }
                gridMap.printMap();

                String dataToSend = "STM|e";
                if (BluetoothConnectionService.isConnected()) {
                    if (messageReceivedTextView.getText().length() > 0) {
                        messageReceivedTextView.append("\n");
                    }
                    messageReceivedTextView.append(dataToSend);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });

        turnBackLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tmp = "a";
                sendMovement(tmp, view.getContext());
                gridMap.invalidate();

                if (!robotView.getIsMoving()) {
                    robotView.addMovement("BL");
                }
                gridMap.printMap();

                String dataToSend = "STM|a";
                if (BluetoothConnectionService.isConnected()) {
                    if (messageReceivedTextView.getText().length() > 0) {
                        messageReceivedTextView.append("\n");
                    }
                    messageReceivedTextView.append(dataToSend);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });

        turnBackRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tmp = "d";
                sendMovement(tmp, view.getContext());
                gridMap.invalidate();

                if (!robotView.getIsMoving()) {
                    robotView.addMovement("BR");
                }
                gridMap.printMap();

                String dataToSend = "STM|d";
                if (BluetoothConnectionService.isConnected()) {
                    if (messageReceivedTextView.getText().length() > 0) {
                        messageReceivedTextView.append("\n");
                    }
                    messageReceivedTextView.append(dataToSend);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });

        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sentText = typeBoxEditText.getText().toString();

                if (TextUtils.isEmpty(sentText)) {
                    return;
                }

                if (messageReceivedTextView.getText().length() > 0) {
                    messageReceivedTextView.append("\n");
                }
                messageReceivedTextView.append(sentText);

                typeBoxEditText.setText("");

                if (BluetoothConnectionService.isConnected()) {
                    sendData(sentText, MainActivity.this);
                } else {
                    Toast.makeText(MainActivity.this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
                }

                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

    }

    private void sendToAlgo(List<Map<String, Object>> algoDetailsList) {
        try {
            // Convert the list to JSON array of arrays
            JSONArray jsonArray = new JSONArray(algoDetailsList);
            String jsonString = jsonArray.toString();

            // Send the JSON string via Bluetooth using your sendData method
            String finalString = "ALGO|" + jsonString;
            sendData(finalString, context);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error sending data", Toast.LENGTH_SHORT).show();
        }
    }

    public void checkPermissions() {

        ArrayList permissionList = new ArrayList<String>();

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.BLUETOOTH);
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (permissionList.size() > 0) {
            String[] stringArray = (String[]) permissionList.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, stringArray,
                    1);
        }
    }

    private void showPopup(ObstacleView obstacle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.popup_image_face, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView imageFaceText = view.findViewById(R.id.image_face_text);
        ImageView robotCarPop = view.findViewById(R.id.robotCar_popup);
        imageFaceText.setVisibility(View.VISIBLE);
        robotCarPop.setVisibility(View.INVISIBLE);
        imageFaceText.setText(String.valueOf(obstacle.getObstacleId()));
        ScrollView scrollView = findViewById(R.id.scroll_view);

        ImageButton[] imageButtons = new ImageButton[4];
        imageButtons[0] = view.findViewById(R.id.up_button);
        imageButtons[1] = view.findViewById(R.id.left_button);
        imageButtons[2] = view.findViewById(R.id.down_button);
        imageButtons[3] = view.findViewById(R.id.right_button);

        for (ImageButton o : imageButtons) {
            o.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int viewId = view.getId();

                    if (viewId == R.id.up_button) {
                        obstacle.setImageFace(FaceDirection.NORTH);
                    } else if (viewId == R.id.left_button) {
                        obstacle.setImageFace(FaceDirection.WEST);
                    } else if (viewId == R.id.right_button) {
                        obstacle.setImageFace(FaceDirection.EAST);
                    } else if (viewId == R.id.down_button) {
                        obstacle.setImageFace(FaceDirection.SOUTH);
                    } else {
                        return;
                    }

                    // send data to remote device
                    String dataToSend = String.format("OBSTACLE %d PLACED AT (%d,%d): %s", obstacle.getObstacleId(),
                            obstacle.getGridX(), obstacle.getGridY(), obstacle.getImageFace().name());

                    // Send the data if Bluetooth is connected
                    if (BluetoothConnectionService.isConnected()) {
                        sendData(dataToSend, MainActivity.this);
                        // Append data to comms box
                        if (messageReceivedTextView.getText().length() > 0) {
                            messageReceivedTextView.append("\n");
                        }
                        messageReceivedTextView.append(dataToSend);

                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
                    }

                    dialog.dismiss();
                }
            });
        }
    }

    private void showPopup(RobotView robotCar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.popup_image_face, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView titleText = view.findViewById(R.id.image_face_popup_title);
        titleText.setText("Select Robot Direction");
        TextView imageFaceText = view.findViewById(R.id.image_face_text);
        ImageView robotCarPop = view.findViewById(R.id.robotCar_popup);
        imageFaceText.setVisibility(View.INVISIBLE);
        robotCarPop.setVisibility(View.VISIBLE);

        ImageButton[] imageButtons = new ImageButton[4];
        imageButtons[0] = view.findViewById(R.id.up_button);
        imageButtons[1] = view.findViewById(R.id.left_button);
        imageButtons[2] = view.findViewById(R.id.down_button);
        imageButtons[3] = view.findViewById(R.id.right_button);

        for (ImageButton o : imageButtons) {
            o.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int viewId = view.getId();
                    String rotation = "";

                    if (viewId == R.id.up_button) {
                        rotation = "0";
                        rotationDetails = "0";
                    } else if (viewId == R.id.left_button) {
                        rotation = "90";
                        rotationDetails = "6";
                    } else if (viewId == R.id.right_button) {
                        rotation = "-90";
                        rotationDetails = "2";
                    } else if (viewId == R.id.down_button) {
                        rotation = "180";
                        rotationDetails = "4";
                    } else {
                        return;
                    }
                    robotCar.setRotation(rotation);
                    dialog.dismiss();
                }
            });
        }
    }

    private void inializeRecycleViews() {
        LocalTime time = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = time.format(formatter);
        responseControl = ResponseControl.getInstance();
        if (responseControl.getResponseList().size() < 1)
            responseControl.add("Awaiting Bluetooth Connection", formattedTime);
        adapterResponse = new AdapterResponse(responseControl.getResponseList(), responseControl.getTimeList());

        SpacingItemDecoration itemDecorationResponse = new SpacingItemDecoration(-20);

        robotStatusControl = RobotStatusControl.getInstance();
        if (robotStatusControl.getRobotStatusList().size() < 1)
            robotStatusControl.add("Awaiting Bluetooth Connection", formattedTime);
        adapterRobotStatus = new AdapterRobotStatus(robotStatusControl.getRobotStatusList(), robotStatusControl.getRobotTimeList());//

        SpacingItemDecoration itemDecorationRobotStatus = new SpacingItemDecoration(-20);
    }

    public void robotStatusAddAndRefresh(String string) {
        LocalTime time = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = time.format(formatter);
        robotStatusControl.add(string, formattedTime);
        adapterRobotStatus.setData(robotStatusControl.getRobotStatusList(), robotStatusControl.getRobotTimeList());
        adapterRobotStatus.notifyDataSetChanged();
    }

    public void responseAddAndRefresh(String string) {
        LocalTime time = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = time.format(formatter);
        responseControl.add(string, formattedTime);
        adapterResponse.setData(responseControl.getResponseList(), responseControl.getTimeList());
        adapterResponse.notifyDataSetChanged();
    }

    public void getTextView(int x, String t) {
        if (x == 1) {
            responseAddAndRefresh(t);
        }
    }

    public ObstacleView getOV(int id) {
        for (ObstacleView obstacleView : obstacleViews) {
            if (obstacleView.getObstacleId() == id)
                return obstacleView;
        }
        return null;
    }

    public RobotView getRV() {
        return robotView;
    }

    public void robotMove(String s) {
        robotView.addMovement(s);
    }

    public void initializeMap() {
        ConstraintLayout constrainMap = (ConstraintLayout) findViewById(R.id.constrain_map);
        ImageView car = (ImageView) findViewById(R.id.robotCar);
        robotView.setCar(car);
        constrainMap.post(new Runnable() {
            @Override
            public void run() {
                if (configOrient == Configuration.ORIENTATION_PORTRAIT) {
                    gridMap.setSides(constrainMap.getWidth() * 0.8);
                    gridMap.invalidate();
                    for (ObstacleView obstacle : obstacleViews) {
                        obstacle.setGridInterval(gridMap.getGridInterval());
                    }
                    robotView.setGridInterval(gridMap.getGridInterval());
                    gridMap.printMap();
                    for (ObstacleView obstacleView : obstacleViews) {
                        obstacleView.getLayoutParams().width = (int) (constrainMap.getWidth() * 0.2);
                        obstacleView.getLayoutParams().height = (int) (constrainMap.getWidth() * 0.2);
                        obstacleView.setOriginalParamHeight((int) (constrainMap.getWidth() * 0.2));
                        obstacleView.setOriginalParamWidth((int) (constrainMap.getWidth() * 0.2));
                    }

                    findViewById(R.id.obstacle_text).getLayoutParams().width = (int) (constrainMap.getWidth() * 0.2);
                    findViewById(R.id.obstacle_text).getLayoutParams().height = (int) (gridMap.getGridInterval() * 6
                            - constrainMap.getWidth() * 0.2);
                    findViewById(R.id.obstacle_box).getLayoutParams().width = (int) (constrainMap.getWidth() * 0.2);
                    findViewById(R.id.obstacle_box).getLayoutParams().height = (int) (constrainMap.getWidth() * 0.2);

                    buttonHolder.getLayoutParams().width = (int) (constrainMap.getWidth() * 0.2);
                    buttonHolder.getLayoutParams().height = (int) (gridMap.getGridInterval() * 14 / 5 * 1.5);
                    ButtonReset.getLayoutParams().width = buttonHolder.getLayoutParams().width;
                    ButtonReset.getLayoutParams().height = buttonHolder.getLayoutParams().height;
                    findViewById(R.id.ll_do_button).getLayoutParams().width = buttonHolder.getLayoutParams().width;
                    findViewById(R.id.ll_do_button).getLayoutParams().height = buttonHolder.getLayoutParams().height;

                    buttonSendFastest.getLayoutParams().width = (int) (constrainMap.getWidth() * 0.2);
                    buttonSendExploration.getLayoutParams().width = (int) (constrainMap.getWidth() * 0.2);

                    LinearLayout llTimeHolder = findViewById(R.id.ll_timer_holder);
                    buttonStart.getLayoutParams().width = llTimeHolder.getLayoutParams().width;

                } else {
                    gridMap.setSides(constrainMap.getHeight() * 0.8);
                    gridMap.invalidate();
                    constrainMap.setMaxWidth((int) (constrainMap.getHeight() * 0.8));
                    for (ObstacleView obstacle : obstacleViews) {
                        obstacle.setGridInterval(gridMap.getGridInterval());
                    }
                    robotView.setGridInterval(gridMap.getGridInterval());
                    gridMap.printMap();
                    for (ObstacleView obstacleView : obstacleViews) {
                        obstacleView.getLayoutParams().width = (int) (constrainMap.getHeight() * 0.2 * 0.9 * 0.95);
                        obstacleView.getLayoutParams().height = (int) (constrainMap.getHeight() * 0.2 * 0.9 * 0.95);
                        obstacleView.setOriginalParamHeight((int) (constrainMap.getHeight() * 0.2 * 0.9 * 0.95));
                        obstacleView.setOriginalParamWidth((int) (constrainMap.getHeight() * 0.2 * 0.9 * 0.95));
                        obstacleView.setX((float) (obstacleView.getX() + constrainMap.getHeight() * 0.2 * 0.9 * 0.025));
                        obstacleView.setY((float) (obstacleView.getY() + constrainMap.getHeight() * 0.2 * 0.9 * 0.025));
                    }

                    findViewById(R.id.obstacle_text).getLayoutParams().width = (int) (constrainMap.getHeight() * 0.2 * 0.9);
                    findViewById(R.id.obstacle_text).getLayoutParams().height = (int) (constrainMap.getHeight() * 0.2 * 0.1);
                    findViewById(R.id.obstacle_box).getLayoutParams().width = (int) (constrainMap.getHeight() * 0.2 * 0.9);
                    findViewById(R.id.obstacle_box).getLayoutParams().height = (int) (constrainMap.getHeight() * 0.2 * 0.9);

                    buttonHolder.getLayoutParams().width = (int) (gridMap.getGridInterval() * 15 / 3);
                    buttonHolder.getLayoutParams().height = (int) (constrainMap.getHeight() * 0.2);
                    ButtonReset.getLayoutParams().width = buttonHolder.getLayoutParams().width;
                    ButtonReset.getLayoutParams().height = buttonHolder.getLayoutParams().width;
                    findViewById(R.id.ll_do_button).getLayoutParams().width = buttonHolder.getLayoutParams().width;
                    findViewById(R.id.ll_do_button).getLayoutParams().height = buttonHolder.getLayoutParams().height;


                    LinearLayout llTimeHolder = findViewById(R.id.ll_timer_holder);
                    buttonStart.getLayoutParams().width = llTimeHolder.getLayoutParams().width;
                }
            }
        });
    }

    public void initializeGrid() {
        obstacleBoxView = findViewById(R.id.obstacle_box);
        gridControl = GridControl.getInstance();
        ScrollView scrollView = findViewById(R.id.scroll_view);

        int[] obstacleIDs = new int[]{
                R.id.obstacle1,
                R.id.obstacle2,
                R.id.obstacle3,
                R.id.obstacle4,
                R.id.obstacle5,
                R.id.obstacle6,
                R.id.obstacle7,
                R.id.obstacle8,
        };
        obstacleViews = new ObstacleView[obstacleIDs.length];
        for (int i = 0; i < obstacleIDs.length; i++) {
            obstacleViews[i] = findViewById(obstacleIDs[i]);
        }
        robotView = findViewById(R.id.robotBound);

        gridMap = (GridView) findViewById(R.id.grid_map);
        gridMap.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                int x = (int) dragEvent.getX() / gridMap.getGridInterval();
                int y = (int) dragEvent.getY() / gridMap.getGridInterval();
                if (dragEvent.getLocalState() instanceof ObstacleView) {
                    switch (dragEvent.getAction()) {
                        case DragEvent.ACTION_DRAG_STARTED:
                            ObstacleView draggedObstacle = (ObstacleView) dragEvent.getLocalState();
                            draggedObstacle.setVisibility(View.INVISIBLE);
                            view.setBackgroundColor(getResources().getColor(R.color.teal_700));
                            if (draggedObstacle.getHasEntered()) {
                                draggedObstacle.imageClear();
                            }
                            view.invalidate();
                            break;
                        case DragEvent.ACTION_DRAG_LOCATION:
                            gridMap.setPrint_back(x, y);
                            break;
                        case DragEvent.ACTION_DRAG_ENTERED:
                            view.setBackgroundColor(getResources().getColor(R.color.teal_700));
                            view.invalidate();
                            break;
                        case DragEvent.ACTION_DRAG_EXITED:
                            view.setBackgroundColor(getResources().getColor(R.color.teal_700));
                            gridMap.cancelPrint_back();
                            view.invalidate();
                            break;
                        case DragEvent.ACTION_DROP:
                            gridMap.cancelPrint_back();
                            ObstacleView droppedObstacle = (ObstacleView) dragEvent.getLocalState();
                            droppedObstacle.shrink();
                            Log.d("GRIDTAG", String.format("Obstacle %d was dropped on the map.",
                                    droppedObstacle.getObstacleId()));
                            int[] movePotential = droppedObstacle.getMovePotential(dragEvent.getX(), dragEvent.getY());
                            if (gridMap.isValidBlockPlacement((int) dragEvent.getX() / gridMap.getGridInterval(),
                                    (int) dragEvent.getY() / gridMap.getGridInterval())) {

//                                // send string via bluetooth to remote device
//                                String dataToSend = String.format("OBSTACLE %d PLACED AT (%d,%d)", droppedObstacle.getObstacleId(),
//                                        droppedObstacle.getGridX(), droppedObstacle.getGridY());
//
//                                // send the data if Bluetooth is connected
//                                if (BluetoothConnectionService.isConnected()) {
//                                    sendData(dataToSend, MainActivity.this);
//                                    // add to comms box
//                                    if (messageReceivedTextView.getText().length() > 0) {
//                                        messageReceivedTextView.append("\n");
//                                    }
//                                    messageReceivedTextView.append(dataToSend);
//
//                                    scrollView.post(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            scrollView.fullScroll(View.FOCUS_DOWN);
//                                        }
//                                    });
//                                } else {
//                                    Toast.makeText(MainActivity.this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
//                                }

                                droppedObstacle.move(dragEvent.getX(), dragEvent.getY());
                                if (!droppedObstacle.getHasEntered()) {
                                    obstacle_count++;
                                }

                                Log.d("GRIDTAG",
                                        String.format("Obstacle %d was moved to (%d, %d) on the map.",
                                                droppedObstacle.getObstacleId(),
                                                droppedObstacle.getGridX(),
                                                droppedObstacle.getGridY()));
                                droppedObstacle.imagePlace();
                                droppedObstacle.setHasEntered(true);
                                showPopup(droppedObstacle);

                            } else {
                                Log.d("GRIDTAG", String.format("Obstacle %d cannot move to (%d, %d) as it is occupied.",
                                        droppedObstacle.getObstacleId(),
                                        movePotential[0],
                                        movePotential[1]));
                                droppedObstacle.reset();
                                if (!droppedObstacle.getHasEntered()) {
                                    droppedObstacle.enlarge();
                                } else {
                                    droppedObstacle.imagePlace();
                                }
                                Log.d("GRIDTAG",
                                        String.format("Obstacle %d was reset.",
                                                droppedObstacle.getObstacleId()));
                            }
                            view.setBackgroundColor(getResources().getColor(R.color.teal_700));
                            view.invalidate();
                            Log.d("DROPPED_OBS", String.format("onDrag: %d %d", droppedObstacle.getLastSnapX(),
                                    droppedObstacle.getLastSnapY()));
                            break;
                        case DragEvent.ACTION_DRAG_ENDED:
                            if (!dragEvent.getResult()) {
                                droppedObstacle = (ObstacleView) dragEvent.getLocalState();
                                Log.d("GRIDTAG",
                                        String.format("Obstacle %d was dropped outside of the map.",
                                                droppedObstacle.getObstacleId()));
                                droppedObstacle.fullReset(orientStr);
                                droppedObstacle.setHasEntered(false);
                                gridMap.printMap();
                                Log.d("GRIDTAG",
                                        String.format("Obstacle %d was reset.",
                                                droppedObstacle.getObstacleId()));
                            }
                            view.setBackgroundColor(getResources().getColor(R.color.teal_700));
                            view.invalidate();
                            break;
                    }
                } else if (dragEvent.getLocalState() instanceof RobotView) {
                    RobotView draggedRobot = (RobotView) dragEvent.getLocalState();
                    switch (dragEvent.getAction()) {
                        case DragEvent.ACTION_DRAG_STARTED: //long press
                            draggedRobot.startDrag();
                            break;
                        case DragEvent.ACTION_DRAG_LOCATION: //Updating location
                            gridMap.setPrint_back_robot(x, y);
                            break;
                        case DragEvent.ACTION_DRAG_ENTERED: //entered map
                            break;
                        case DragEvent.ACTION_DROP: //dropped in map
                            draggedRobot.stopDrag();
                            if (x == 1) x = 2;
                            if (x == 20) x = 19;
                            if (y == 0) y = 1;
                            if (y == 19) y = 18;
                            if (x > 1 && x < 20 && y > 0 && y < 19) {
                                draggedRobot.teleport(x - 1, 19 - y, "0");
                            }
                            gridMap.cancelPrint_back_robot();
                            showPopup(draggedRobot);
                            break;
                        case DragEvent.ACTION_DRAG_ENDED: //check if dropped out of map
                            if (!dragEvent.getResult()) {
                                draggedRobot.stopDrag();
                                gridMap.cancelPrint_back_robot();
                            }
                            break;
                    }
                }
                return true;
            }
        });
    }

    public void reinit() {
        initializeGrid();
        initializeButtons();
        initializeMap();
        inializeRecycleViews();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configOrient = newConfig.orientation;
        if (configOrient == Configuration.ORIENTATION_PORTRAIT) {
            orientStr = "p";
            setContentView(R.layout.activity_main);
            reinit();
        }
    }
    private void startTimer() {
        if (isTimerRunning) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isTimerRunning) {
                        long currentTimeMillis = System.currentTimeMillis();
                        long elapsedTimeMillis = currentTimeMillis - startTimeMillis;
                        milliseconds = (int) (elapsedTimeMillis % 1000);
                        seconds = (int) (elapsedTimeMillis / 1000) % 60;
                        minutes = (int) (elapsedTimeMillis / (1000 * 60));

                        String time = String.format("Timer: %02d:%02d:%03d", minutes, seconds, milliseconds);
                        textTimer.setText(time);
                        handler.postDelayed(this, 1); // Update every millisecond
                    }
                }
            }, 1);
        }
    }
    private void stopTimer() {
        isTimerRunning = false;
    }
    private void resetTimer() {
        stopTimer();
        minutes = 0;
        seconds = 0;
        milliseconds = 0;
        String time = String.format("Timer: %02d:%02d:%03d", minutes, seconds, milliseconds);
        textTimer.setText(time);
    }

    public static void sharedPreferences() {
        SharedPreferences sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);
        editor = sharedPreferences.edit();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    private final BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences();

            if(status.equals("connected")){
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(MainActivity.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(MainActivity.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Disconnected");
                myDialog.show();
            }
            editor.commit();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1:
                if(resultCode == Activity.RESULT_OK){
                    mBTDevice = (BluetoothDevice) data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        try{
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString(TAG, "onSaveInstanceState");
        showLog("Exiting onSaveInstanceState");
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

}