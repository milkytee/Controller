package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class ExcavatorPostureView extends View {
    private Paint paint;
    private Paint shadowPaint;
    private float boomAngle = -30f;  // 大臂角度
    private float stickAngle = 45f;  // 小臂角度
    private float bucketAngle = 10f; // 铲斗角度
    
    // 挖机各部分长度（相对值）
    private float boomLength = 0.4f;
    private float stickLength = 0.3f;
    private float bucketLength = 0.15f;
    
    public ExcavatorPostureView(Context context) {
        super(context);
        init();
    }
    
    public ExcavatorPostureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.parseColor("#40000000"));
        shadowPaint.setStyle(Paint.Style.FILL);
    }
    
    public void setAngles(float boom, float stick, float bucket) {
        this.boomAngle = boom;
        this.stickAngle = stick;
        this.bucketAngle = bucket;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        // 计算中心点和缩放比例（左对齐）
        float centerX = width * 0.25f;  // 从左边开始，而不是居中
        float centerY = height * 0.75f;
        float scale = Math.min(width, height) * 0.85f;
        
        // 绘制底盘和履带
        drawChassis(canvas, centerX, centerY, scale);
        
        // 绘制驾驶室
        drawCab(canvas, centerX, centerY - scale * 0.05f, scale);
        
        // 绘制大臂（Boom）
        float boomEndX = centerX + (float) (Math.cos(Math.toRadians(boomAngle)) * boomLength * scale);
        float boomEndY = centerY - scale * 0.05f - (float) (Math.sin(Math.toRadians(boomAngle)) * boomLength * scale);
        drawBoom(canvas, centerX, centerY - scale * 0.05f, boomEndX, boomEndY, scale);
        
        // 绘制小臂（Stick）
        float stickAngleTotal = boomAngle + stickAngle;
        float stickStartX = boomEndX;
        float stickStartY = boomEndY;
        float stickEndX = stickStartX + (float) (Math.cos(Math.toRadians(stickAngleTotal)) * stickLength * scale);
        float stickEndY = stickStartY - (float) (Math.sin(Math.toRadians(stickAngleTotal)) * stickLength * scale);
        drawStick(canvas, stickStartX, stickStartY, stickEndX, stickEndY, scale);
        
        // 绘制铲斗（Bucket）
        float bucketAngleTotal = stickAngleTotal + bucketAngle;
        float bucketStartX = stickEndX;
        float bucketStartY = stickEndY;
        drawBucket(canvas, bucketStartX, bucketStartY, bucketAngleTotal, scale);
    }
    
    private void drawChassis(Canvas canvas, float x, float y, float scale) {
        // 绘制履带阴影
        float trackWidth = scale * 0.25f;
        float trackHeight = scale * 0.08f;
        canvas.drawRoundRect(x - trackWidth/2 - 2, y - trackHeight/2 - 2, 
                           x + trackWidth/2 + 2, y + trackHeight/2 + 2, 4, 4, shadowPaint);
        
        // 绘制履带（深灰色）
        paint.setColor(Color.parseColor("#333333"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(x - trackWidth/2, y - trackHeight/2, 
                           x + trackWidth/2, y + trackHeight/2, 4, 4, paint);
        
        // 履带纹理（小矩形）
        paint.setColor(Color.parseColor("#222222"));
        float segmentWidth = trackWidth / 8;
        for (int i = 0; i < 8; i++) {
            float left = x - trackWidth/2 + i * segmentWidth;
            canvas.drawRect(left, y - trackHeight/2, left + segmentWidth * 0.8f, y + trackHeight/2, paint);
        }
    }
    
    private void drawCab(Canvas canvas, float x, float y, float scale) {
        float cabWidth = scale * 0.18f;
        float cabHeight = scale * 0.15f;
        
        // 驾驶室阴影
        canvas.drawRoundRect(x - cabWidth/2 + 2, y - cabHeight + 2, 
                           x + cabWidth/2 + 2, y + 2, 6, 6, shadowPaint);
        
        // 驾驶室主体（渐变）
        LinearGradient cabGradient = new LinearGradient(
            x - cabWidth/2, y - cabHeight,
            x - cabWidth/2, y,
            Color.parseColor("#FFA500"),
            Color.parseColor("#CC7700"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(cabGradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(x - cabWidth/2, y - cabHeight, x + cabWidth/2, y, 6, 6, paint);
        paint.setShader(null);
        
        // 驾驶室边框
        paint.setColor(Color.parseColor("#AA6600"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        canvas.drawRoundRect(x - cabWidth/2, y - cabHeight, x + cabWidth/2, y, 6, 6, paint);
        
        // 驾驶室窗户
        paint.setColor(Color.parseColor("#1A1A1A"));
        paint.setStyle(Paint.Style.FILL);
        float windowWidth = cabWidth * 0.6f;
        float windowHeight = cabHeight * 0.4f;
        canvas.drawRoundRect(x - windowWidth/2, y - cabHeight * 0.7f, 
                           x + windowWidth/2, y - cabHeight * 0.7f + windowHeight, 3, 3, paint);
        
        // 窗户边框
        paint.setColor(Color.parseColor("#666666"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawRoundRect(x - windowWidth/2, y - cabHeight * 0.7f, 
                           x + windowWidth/2, y - cabHeight * 0.7f + windowHeight, 3, 3, paint);
    }
    
    private void drawBoom(Canvas canvas, float startX, float startY, float endX, float endY, float scale) {
        // 计算大臂角度和长度
        double angle = Math.atan2(startY - endY, endX - startX);
        float length = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        float boomWidth = scale * 0.03f;
        
        // 大臂阴影
        float shadowOffset = 3f;
        Path shadowPath = new Path();
        shadowPath.moveTo(startX + shadowOffset, startY + shadowOffset);
        shadowPath.lineTo(startX + (float)(Math.cos(angle) * length) + shadowOffset, 
                         startY - (float)(Math.sin(angle) * length) + shadowOffset);
        shadowPath.lineTo(startX + (float)(Math.cos(angle) * length) + shadowOffset - (float)(Math.cos(angle + Math.PI/2) * boomWidth), 
                         startY - (float)(Math.sin(angle) * length) + shadowOffset - (float)(Math.sin(angle + Math.PI/2) * boomWidth));
        shadowPath.lineTo(startX - (float)(Math.cos(angle + Math.PI/2) * boomWidth) + shadowOffset, 
                         startY - (float)(Math.sin(angle + Math.PI/2) * boomWidth) + shadowOffset);
        shadowPath.close();
        canvas.drawPath(shadowPath, shadowPaint);
        
        // 大臂主体（渐变）
        Path boomPath = new Path();
        boomPath.moveTo(startX, startY);
        boomPath.lineTo(endX, endY);
        boomPath.lineTo(endX - (float)(Math.cos(angle + Math.PI/2) * boomWidth), 
                       endY - (float)(Math.sin(angle + Math.PI/2) * boomWidth));
        boomPath.lineTo(startX - (float)(Math.cos(angle + Math.PI/2) * boomWidth), 
                       startY - (float)(Math.sin(angle + Math.PI/2) * boomWidth));
        boomPath.close();
        
        LinearGradient boomGradient = new LinearGradient(
            startX, startY, endX, endY,
            Color.parseColor("#AAAAAA"),
            Color.parseColor("#666666"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(boomGradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(boomPath, paint);
        paint.setShader(null);
        
        // 大臂边框
        paint.setColor(Color.parseColor("#555555"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawPath(boomPath, paint);
        
        // 大臂连接点（液压缸）
        RadialGradient jointGradient = new RadialGradient(
            startX, startY, boomWidth * 1.5f,
            Color.parseColor("#888888"),
            Color.parseColor("#444444"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(jointGradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(startX, startY, boomWidth * 1.5f, paint);
        canvas.drawCircle(endX, endY, boomWidth * 1.2f, paint);
        paint.setShader(null);
    }
    
    private void drawStick(Canvas canvas, float startX, float startY, float endX, float endY, float scale) {
        // 计算小臂角度和长度
        double angle = Math.atan2(startY - endY, endX - startX);
        float length = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        float stickWidth = scale * 0.025f;
        
        // 小臂阴影
        float shadowOffset = 2f;
        Path shadowPath = new Path();
        shadowPath.moveTo(startX + shadowOffset, startY + shadowOffset);
        shadowPath.lineTo(startX + (float)(Math.cos(angle) * length) + shadowOffset, 
                         startY - (float)(Math.sin(angle) * length) + shadowOffset);
        shadowPath.lineTo(startX + (float)(Math.cos(angle) * length) + shadowOffset - (float)(Math.cos(angle + Math.PI/2) * stickWidth), 
                         startY - (float)(Math.sin(angle) * length) + shadowOffset - (float)(Math.sin(angle + Math.PI/2) * stickWidth));
        shadowPath.lineTo(startX - (float)(Math.cos(angle + Math.PI/2) * stickWidth) + shadowOffset, 
                         startY - (float)(Math.sin(angle + Math.PI/2) * stickWidth) + shadowOffset);
        shadowPath.close();
        canvas.drawPath(shadowPath, shadowPaint);
        
        // 小臂主体（渐变）
        Path stickPath = new Path();
        stickPath.moveTo(startX, startY);
        stickPath.lineTo(endX, endY);
        stickPath.lineTo(endX - (float)(Math.cos(angle + Math.PI/2) * stickWidth), 
                        endY - (float)(Math.sin(angle + Math.PI/2) * stickWidth));
        stickPath.lineTo(startX - (float)(Math.cos(angle + Math.PI/2) * stickWidth), 
                        startY - (float)(Math.sin(angle + Math.PI/2) * stickWidth));
        stickPath.close();
        
        LinearGradient stickGradient = new LinearGradient(
            startX, startY, endX, endY,
            Color.parseColor("#FFA500"),
            Color.parseColor("#CC7700"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(stickGradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(stickPath, paint);
        paint.setShader(null);
        
        // 小臂边框
        paint.setColor(Color.parseColor("#AA6600"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawPath(stickPath, paint);
        
        // 小臂连接点
        RadialGradient jointGradient = new RadialGradient(
            startX, startY, stickWidth * 1.3f,
            Color.parseColor("#FFA500"),
            Color.parseColor("#AA6600"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(jointGradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(startX, startY, stickWidth * 1.3f, paint);
        canvas.drawCircle(endX, endY, stickWidth * 1.1f, paint);
        paint.setShader(null);
    }
    
    private void drawBucket(Canvas canvas, float startX, float startY, float angle, float scale) {
        float bucketLen = bucketLength * scale;
        float endX = startX + (float) (Math.cos(Math.toRadians(angle)) * bucketLen);
        float endY = startY - (float) (Math.sin(Math.toRadians(angle)) * bucketLen);
        float bucketWidth = scale * 0.1f;
        
        // 铲斗阴影
        Path shadowPath = new Path();
        float perpAngle = angle + 90;
        float offsetX = (float) (Math.cos(Math.toRadians(perpAngle)) * bucketWidth / 2);
        float offsetY = (float) (-Math.sin(Math.toRadians(perpAngle)) * bucketWidth / 2);
        shadowPath.moveTo(startX + offsetX + 2, startY + offsetY + 2);
        shadowPath.lineTo(startX - offsetX + 2, startY - offsetY + 2);
        shadowPath.lineTo(endX - offsetX + 2, endY - offsetY + 2);
        shadowPath.lineTo(endX + offsetX + 2, endY + offsetY + 2);
        shadowPath.close();
        canvas.drawPath(shadowPath, shadowPaint);
        
        // 铲斗主体（渐变）
        Path bucketPath = new Path();
        bucketPath.moveTo(startX + offsetX, startY + offsetY);
        bucketPath.lineTo(startX - offsetX, startY - offsetY);
        bucketPath.lineTo(endX - offsetX, endY - offsetY);
        bucketPath.lineTo(endX + offsetX, endY + offsetY);
        bucketPath.close();
        
        LinearGradient bucketGradient = new LinearGradient(
            startX, startY, endX, endY,
            Color.parseColor("#FFD700"),
            Color.parseColor("#CCAA00"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(bucketGradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(bucketPath, paint);
        paint.setShader(null);
        
        // 铲斗边框
        paint.setColor(Color.parseColor("#AA8800"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        canvas.drawPath(bucketPath, paint);
        
        // 铲斗齿（3个）
        paint.setColor(Color.parseColor("#888888"));
        paint.setStyle(Paint.Style.FILL);
        float toothLength = scale * 0.02f;
        for (int i = 0; i < 3; i++) {
            float t = (i + 1) / 4.0f;
            float toothX = startX + (endX - startX) * t;
            float toothY = startY + (endY - startY) * t;
            float toothEndX = toothX + (float) (Math.cos(Math.toRadians(angle)) * toothLength);
            float toothEndY = toothY - (float) (Math.sin(Math.toRadians(angle)) * toothLength);
            canvas.drawLine(toothX + offsetX, toothY + offsetY, toothEndX + offsetX, toothEndY + offsetY, paint);
            canvas.drawLine(toothX - offsetX, toothY - offsetY, toothEndX - offsetX, toothEndY - offsetY, paint);
        }
        
        // 连接点
        RadialGradient jointGradient = new RadialGradient(
            startX, startY, scale * 0.015f,
            Color.parseColor("#FFA500"),
            Color.parseColor("#CC7700"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(jointGradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(startX, startY, scale * 0.015f, paint);
        paint.setShader(null);
    }
}
