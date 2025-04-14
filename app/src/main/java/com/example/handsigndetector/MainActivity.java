// MainActivity.java
package com.example.handsigndetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.hands.Hand;
import com.google.mlkit.vision.hands.HandLandmark;
import com.google.mlkit.vision.hands.HandsOptions;
import com.google.mlkit.vision.hands.HandsResult;
import com.google.mlkit.vision.hands.Hands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HandSignDetector";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView previewView;
    private TextView detectionTextView;
    private ExecutorService cameraExecutor;
    private Hands hands;
    private SoundPool soundPool;
    private Map<String, Integer> soundMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        detectionTextView = findViewById(R.id.detection_text);

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Initialize audio
        initializeSoundPool();
        
        // Initialize the hand detector
        initializeHandDetector();

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void initializeSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        // Load the sounds for different hand signs
        soundMap = new HashMap<>();
        soundMap.put("OPEN_PALM", soundPool.load(this, R.raw.note_c, 1));
        soundMap.put("CLOSED_FIST", soundPool.load(this, R.raw.note_d, 1));
        soundMap.put("POINTING_UP", soundPool.load(this, R.raw.note_e, 1));
        soundMap.put("VICTORY", soundPool.load(this, R.raw.note_f, 1));
        soundMap.put("THUMB_UP", soundPool.load(this, R.raw.note_g, 1));
        soundMap.put("PINKY_OUT", soundPool.load(this, R.raw.note_a, 1));
        soundMap.put("ROCK_ON", soundPool.load(this, R.raw.note_b, 1));
    }

    private void initializeHandDetector() {
        HandsOptions options = new HandsOptions.Builder()
                .setStaticImageMode(false)
                .setMaxNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build();

        hands = Hands.getClient(options);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new HandSignAnalyzer());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private class HandSignAnalyzer implements ImageAnalysis.Analyzer {
        private long lastPlayTime = 0;
        private String lastDetectedSign = "";
        private long lastProcessingTimeMs = 0;
        private static final long PROCESSING_INTERVAL_MS = 100; // Process every 100ms

        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            long currentTimeMs = System.currentTimeMillis();
            
            // Only process frames at a specific interval
            if (currentTimeMs - lastProcessingTimeMs < PROCESSING_INTERVAL_MS) {
                imageProxy.close();
                return;
            }
            
            lastProcessingTimeMs = currentTimeMs;
            
            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(), 
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            hands.process(image)
                    .addOnSuccessListener(handResults -> {
                        if (!handResults.isEmpty()) {
                            // Detect hand sign based on hand landmarks
                            String detectedSign = identifyHandSign(handResults);
                            
                            if (!detectedSign.equals(lastDetectedSign)) {
                                // Play corresponding sound if it's a new sign
                                // and enough time has passed (prevent rapid repetition)
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastPlayTime > 500) {
                                    playSound(detectedSign);
                                    lastPlayTime = currentTime;
                                    lastDetectedSign = detectedSign;
                                }
                            }
                            
                            // Update UI
                            runOnUiThread(() -> {
                                detectionTextView.setText("Detected: " + detectedSign);
                            });
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Hand detection failed: " + e.getMessage());
                        imageProxy.close();
                    });
        }

        private String identifyHandSign(HandsResult handResults) {
            Hand hand = handResults.get(0);
            List<HandLandmark> landmarks = hand.getLandmarks();
            
            if (HandSignDetector.isOpenPalm(landmarks)) {
                return "OPEN_PALM";
            } else if (HandSignDetector.isClosedFist(landmarks)) {
                return "CLOSED_FIST";
            } else if (HandSignDetector.isPointingUp(landmarks)) {
                return "POINTING_UP";
            } else if (HandSignDetector.isVictorySign(landmarks)) {
                return "VICTORY";
            } else if (HandSignDetector.isThumbUp(landmarks)) {
                return "THUMB_UP";
            } else if (HandSignDetector.isPinkyOut(landmarks)) {
                return "PINKY_OUT";
            } else if (HandSignDetector.isRockOn(landmarks)) {
                return "ROCK_ON";
            }
            
            return "UNKNOWN";
        }
    }

    private void playSound(String handSign) {
        Integer soundId = soundMap.get(handSign);
        if (soundId != null) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", 
                              Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        cameraExecutor.shutdown();
    }
}