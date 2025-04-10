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
import com.google.mlkit.vision.hand.HandLandmark;
import com.google.mlkit.vision.hand.HandLandmarker;
import com.google.mlkit.vision.hand.HandLandmarkerOptions;
import com.google.mlkit.vision.hand.hands.HandsOptions;
import com.google.mlkit.vision.hands.Hands;
import com.google.mlkit.vision.hands.HandsOptions;

import java.util.HashMap;
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

        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
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
            // This is a simplified hand sign detection
            // In a real app, you would implement more sophisticated logic
            // based on the relative positions of hand landmarks
            
            // Example classification logic:
            Hand hand = handResults.get(0);
            List<HandLandmark> landmarks = hand.getLandmarks();
            
            // Simplified detection - in reality you would need more sophisticated algorithms
            if (isOpenPalm(landmarks)) {
                return "OPEN_PALM";
            } else if (isClosedFist(landmarks)) {
                return "CLOSED_FIST";
            } else if (isPointingUp(landmarks)) {
                return "POINTING_UP";
            } else if (isVictorySign(landmarks)) {
                return "VICTORY";
            } else if (isThumbUp(landmarks)) {
                return "THUMB_UP";
            }
            
            return "UNKNOWN";
        }
        
        // These methods would contain the actual detection logic
        // based on landmark positions
        private boolean isOpenPalm(List<HandLandmark> landmarks) {
            // Implementation for open palm detection
            // Example: Check if all fingers are extended
            return false; // Placeholder
        }
        
        private boolean isClosedFist(List<HandLandmark> landmarks) {
            // Implementation for closed fist detection
            return false; // Placeholder
        }
        
        private boolean isPointingUp(List<HandLandmark> landmarks) {
            // Implementation for pointing up gesture
            return false; // Placeholder
        }
        
        private boolean isVictorySign(List<HandLandmark> landmarks) {
            // Implementation for victory sign (V sign)
            return false; // Placeholder
        }
        
        private boolean isThumbUp(List<HandLandmark> landmarks) {
            // Implementation for thumbs up gesture
            return false; // Placeholder
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
