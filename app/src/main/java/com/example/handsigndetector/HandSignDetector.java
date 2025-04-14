// HandSignDetector.java
// This file contains the implementation for hand sign detection algorithms
// Add this as a separate class in your project

package com.example.handsigndetector;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.hands.HandLandmark;

import java.util.List;

public class HandSignDetector {

    // Constants for detection thresholds
    private static final float FINGER_OPEN_THRESHOLD = 0.1f;
    private static final float FINGER_CLOSED_THRESHOLD = 0.05f;

    // Implement these methods and replace the placeholder methods in MainActivity
    
    public static boolean isOpenPalm(List<HandLandmark> landmarks) {
        // Open palm: all fingers are extended
        return areAllFingersOpen(landmarks);
    }
    
    public static boolean isClosedFist(List<HandLandmark> landmarks) {
        // Closed fist: all fingers are closed
        return !isAnyFingerOpen(landmarks);
    }
    
    public static boolean isPointingUp(List<HandLandmark> landmarks) {
        // Pointing up: index finger extended, other fingers closed
        return isIndexFingerOpen(landmarks) && 
               !isMiddleFingerOpen(landmarks) && 
               !isRingFingerOpen(landmarks) && 
               !isPinkyFingerOpen(landmarks);
    }
    
    public static boolean isVictorySign(List<HandLandmark> landmarks) {
        // V sign: index and middle fingers extended, other fingers closed
        return isIndexFingerOpen(landmarks) && 
               isMiddleFingerOpen(landmarks) && 
               !isRingFingerOpen(landmarks) && 
               !isPinkyFingerOpen(landmarks);
    }
    
    public static boolean isThumbUp(List<HandLandmark> landmarks) {
        // Thumb up: thumb extended upward, other fingers closed
        if (landmarks.size() < HandLandmark.THUMB_TIP + 1) {
            return false;
        }
        
        PointF3D wrist = landmarks.get(HandLandmark.WRIST).getPosition();
        PointF3D thumbTip = landmarks.get(HandLandmark.THUMB_TIP).getPosition();
        
        // Thumb is pointing up if y coordinate of tip is significantly less than wrist
        // (Note: in image coordinates, y increases downward)
        boolean thumbPointingUp = (wrist.getY() - thumbTip.getY()) > 0.15;
        
        return thumbPointingUp && 
               !isIndexFingerOpen(landmarks) && 
               !isMiddleFingerOpen(landmarks) && 
               !isRingFingerOpen(landmarks) && 
               !isPinkyFingerOpen(landmarks);
    }
    
    public static boolean isPinkyOut(List<HandLandmark> landmarks) {
        // Pinky out: pinky finger extended, other fingers closed (like drinking tea)
        return !isIndexFingerOpen(landmarks) && 
               !isMiddleFingerOpen(landmarks) && 
               !isRingFingerOpen(landmarks) && 
               isPinkyFingerOpen(landmarks);
    }
    
    public static boolean isRockOn(List<HandLandmark> landmarks) {
        // Rock on sign: index and pinky extended, other fingers closed
        return isIndexFingerOpen(landmarks) && 
               !isMiddleFingerOpen(landmarks) && 
               !isRingFingerOpen(landmarks) && 
               isPinkyFingerOpen(landmarks);
    }
    
    // Helper methods
    
    private static boolean areAllFingersOpen(List<HandLandmark> landmarks) {
        return isThumbOpen(landmarks) && 
               isIndexFingerOpen(landmarks) && 
               isMiddleFingerOpen(landmarks) && 
               isRingFingerOpen(landmarks) && 
               isPinkyFingerOpen(landmarks);
    }
    
    private static boolean isAnyFingerOpen(List<HandLandmark> landmarks) {
        return isThumbOpen(landmarks) || 
               isIndexFingerOpen(landmarks) || 
               isMiddleFingerOpen(landmarks) || 
               isRingFingerOpen(landmarks) || 
               isPinkyFingerOpen(landmarks);
    }
    
    private static boolean isThumbOpen(List<HandLandmark> landmarks) {
        if (landmarks.size() < HandLandmark.THUMB_TIP + 1) {
            return false;
        }
        
        PointF3D mcp = landmarks.get(HandLandmark.THUMB_CMC).getPosition();
        PointF3D ip = landmarks.get(HandLandmark.THUMB_IP).getPosition();
        PointF3D tip = landmarks.get(HandLandmark.THUMB_TIP).getPosition();
        
        float distance = distance(mcp, tip);
        float jointDistance = distance(mcp, ip);
        
        return distance > jointDistance * 1.5;
    }
    
    private static boolean isIndexFingerOpen(List<HandLandmark> landmarks) {
        return isFingerOpen(landmarks, HandLandmark.INDEX_FINGER_MCP, HandLandmark.INDEX_FINGER_PIP, HandLandmark.INDEX_FINGER_TIP);
    }
    
    private static boolean isMiddleFingerOpen(List<HandLandmark> landmarks) {
        return isFingerOpen(landmarks, HandLandmark.MIDDLE_FINGER_MCP, HandLandmark.MIDDLE_FINGER_PIP, HandLandmark.MIDDLE_FINGER_TIP);
    }
    
    private static boolean isRingFingerOpen(List<HandLandmark> landmarks) {
        return isFingerOpen(landmarks, HandLandmark.RING_FINGER_MCP, HandLandmark.RING_FINGER_PIP, HandLandmark.RING_FINGER_TIP);
    }
    
    private static boolean isPinkyFingerOpen(List<HandLandmark> landmarks) {
        return isFingerOpen(landmarks, HandLandmark.PINKY_MCP, HandLandmark.PINKY_PIP, HandLandmark.PINKY_TIP);
    }
    
    private static boolean isFingerOpen(List<HandLandmark> landmarks, int mcpIndex, int pipIndex, int tipIndex) {
        if (landmarks.size() < Math.max(Math.max(mcpIndex, pipIndex), tipIndex) + 1) {
            return false;
        }
        
        PointF3D mcp = landmarks.get(mcpIndex).getPosition();
        PointF3D pip = landmarks.get(pipIndex).getPosition();
        PointF3D tip = landmarks.get(tipIndex).getPosition();
        
        float distanceMcpTip = distance(mcp, tip);
        float distanceMcpPip = distance(mcp, pip);
        
        // If the finger is extended, the tip will be far from the base
        return distanceMcpTip > distanceMcpPip * 1.5;
    }
    
    private static float distance(PointF3D p1, PointF3D p2) {
        float dx = p1.getX() - p2.getX();
        float dy = p1.getY() - p2.getY();
        float dz = p1.getZ() - p2.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}