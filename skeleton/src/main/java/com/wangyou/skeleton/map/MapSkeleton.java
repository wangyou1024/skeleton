package com.wangyou.skeleton.map;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.PathParser;

import com.wangyou.skeleton.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MapSkeleton extends View {
    protected Path path;
    private RectF rectF;

    private float process;

    // 属性
    private int mBackground = Color.GRAY;
    private float angle = 30;
    private int lightColor = Color.WHITE;
    private int strokeColor = Color.BLUE;
    private float strokeWidth = 1;

    private int duration = 3000;

    private String cityResource = "";

    private String cityName = "";
    private int cityNameColor = Color.BLACK;
    private float cityNameSize = 15;

    private CreatePath createPath;

    public MapSkeleton(Context context) {
        super(context);
    }

    public MapSkeleton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.mapSkeletonStyle);
    }

    public MapSkeleton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.DefaultMapSkeleton);
    }

    public MapSkeleton(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MapSkeleton, defStyleAttr, defStyleRes);
        if (typedArray == null) {
            return;
        }
        mBackground = typedArray.getColor(R.styleable.MapSkeleton_map_skeleton_background, Color.GRAY);
        angle = typedArray.getFloat(R.styleable.MapSkeleton_map_skeleton_light_angle, 30);
        lightColor = typedArray.getColor(R.styleable.MapSkeleton_map_skeleton_light_color, Color.WHITE);
        strokeColor = typedArray.getColor(R.styleable.MapSkeleton_map_skeleton_stroke_color, Color.BLUE);
        strokeWidth = typedArray.getDimensionPixelSize(R.styleable.MapSkeleton_map_skeleton_stroke_width, 1);
        duration = typedArray.getInt(R.styleable.MapSkeleton_map_skeleton_duration, 1);
        cityResource = typedArray.getString(R.styleable.MapSkeleton_map_skeleton_city_source);
        cityName = typedArray.getString(R.styleable.MapSkeleton_map_skeleton_city_name);
        cityNameColor = typedArray.getColor(R.styleable.MapSkeleton_map_skeleton_city_name_color, Color.BLACK);
        cityNameSize = typedArray.getDimensionPixelSize(R.styleable.MapSkeleton_map_skeleton_city_name_size, 15);
        typedArray.recycle();
        getPath(cityResource);
        loadAnimation();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN: clickAnimation();break;
            case MotionEvent.ACTION_UP:
                if (clickAnimator != null){
                    clickAnimator.start();
                }
                break;
        }
        super.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.AT_MOST) {
            // 宽是精确值，动态计算高
            setMeasuredDimension(width, (int) (width * (rectF.height() / rectF.width())));
        } else if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY) {
            // 高是精确值，动态计算宽
            setMeasuredDimension((int) (height * (rectF.width() / rectF.height())), height);
        } else if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(width, height);
        } else {
            // 无精确值，用原始大小
            setMeasuredDimension((int) rectF.width(), (int) rectF.height());
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float height = rectF.height();
        float width = rectF.width();
        float scaleX = getMeasuredWidth() / width;
        float scaleY = getMeasuredHeight() / height;
        // 选择较小的比例，避免超出范围，即scale*width>scaleX*width=getMeasuredWidth
        float scale = Math.min(scaleX, scaleY);
        // 保存画布的正常状态
        canvas.save();
        // 偏移画布，一般路径可能是经纬度，路径的原点是地图坐标系的原点，
        // 为了能正常显示，需要移动和缩放画布来使画布原点与坐标系原点重合，大小也能刚好填满画布
        if (scale == scaleX) {
            // 高的比例较大，会出现空白，居中需要向下移动
            canvas.translate(-rectF.left * scale, -rectF.top * scale + (getMeasuredHeight() - (height * scale)) / 2);
        } else {
            canvas.translate(-rectF.left * scale + (getMeasuredWidth() - (width * scale)) / 2, -rectF.top * scale);
        }
        canvas.scale(scale, scale);
        //先画背景
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(mBackground);
        canvas.drawPath(path, backgroundPaint);

        // 恢复正常画布状态，以绘制文字
        // 不恢复状态绘制文字时，需要把字体大小cityNameSize除以scale，因为画布被放大了scale，
        // 但是放大过程并没有如预期那样变为两个正常的字体，反而重叠在了一起
        canvas.restore();
        // 文字
        if (!TextUtils.isEmpty(cityName)){
            Paint cityNamePaint = new Paint();
            cityNamePaint.setTextAlign(Paint.Align.LEFT);
            cityNamePaint.setTextSize(cityNameSize);
            cityNamePaint.setColor(cityNameColor);
            // 大小为int值，此时先计算正常状态下的宽高，然后再进行缩放
            Rect bounds = new Rect();
            cityNamePaint.getTextBounds(cityName, 0, cityName.length(), bounds);
            canvas.drawText(cityName,
                    getMeasuredWidth()/2f-(bounds.width()/2f),
                    getMeasuredHeight()/2f+(bounds.height()/2f),
                    cityNamePaint);
        }

        if (scale == scaleX) {
            // 高的比例较大，会出现空白，居中需要向下移动
            canvas.translate(-rectF.left * scale, -rectF.top * scale + (getMeasuredHeight() - (height * scale)) / 2);
        } else {
            canvas.translate(-rectF.left * scale + (getMeasuredWidth() - (width * scale)) / 2, -rectF.top * scale);
        }
        canvas.scale(scale, scale);

        // 再画动画亮条
        Paint loadPaint = new Paint();
        loadPaint.setAntiAlias(true);
        loadPaint.setStyle(Paint.Style.FILL);
        float angleLength = (float) (rectF.width() * Math.tan(Math.toRadians(angle)));
        float start = -rectF.width() / 2 // 亮条渐变在0.5处，所以需要偏移
                - angleLength // 没有角度时，亮条是垂直的，有角度后需要添加偏移
                + (rectF.width() + 2 * angleLength) * process;
        loadPaint.setShader(new LinearGradient(rectF.left + start, rectF.top, rectF.right + start, rectF.top + angleLength,
                new int[]{0x00ffffff, lightColor, lightColor, 0x00ffffff},
                new float[]{0.45f, 0.499f, 0.501f, 0.55f},
                Shader.TileMode.CLAMP));
        canvas.drawPath(path, loadPaint);

        // 再画边界
        Paint pathPaint = new Paint();
        pathPaint.setColor(strokeColor);
        pathPaint.setStrokeWidth(strokeWidth / scale);
        pathPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, pathPaint);
    }

    private void getPath(String cityResource){
        if (!TextUtils.isEmpty(cityResource) && cityResource.indexOf('.') == -1){
            getPathForProvince(cityResource);
        }else{
            getPathFromAssets(cityResource);
        }
    }

    private void getPathForProvince(String city) {
        try {
            if (isInEditMode()) {
                defaultPath();
                return;
            }
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(getResources().openRawResource(R.raw.chinahigh));
            Element node = document.getElementById(city);
            path = PathParser.createPathFromPathData(node.getAttribute("android:pathData"));
            rectF = new RectF();
            path.computeBounds(rectF, true);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            defaultPath();
        }
    }

    public void getPathFromAssets(String resource) {
            if (isInEditMode()) {
                defaultPath();
                return;
            }
            createPath = () -> {
                try {
                    InputStream inputStream = getResources().getAssets().open(resource);
                    byte[] bytes = new byte[inputStream.available()];
                    inputStream.read(bytes);
                    inputStream.close();
                    String pathStr = new String(bytes, StandardCharsets.UTF_8);
                    String[] pathStrArray = pathStr.split("_");
                    float[][] pathData = new float[pathStrArray.length][2];
                    for (int i = 0; i < pathStrArray.length; i++) {
                        String[] positions = pathStrArray[i].split(",");
                        pathData[i][0] = Float.parseFloat(positions[0].trim());
                        pathData[i][1] = Float.parseFloat(positions[1].trim());
                    }
                    return pathData;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return new float[0][0];
            };
            getPathFromData();
    }

    public void getPathFromData(){
        try{
            float[][] pathData = createPath.getPath();
            path = new Path();
            for (int i = 0; i < pathData.length; i++) {
                if (i == 0) {
                    path.moveTo(pathData[i][0], pathData[i][1]);
                } else {
                    path.lineTo(pathData[i][0], pathData[i][1]);
                }
            }
            path.lineTo(pathData[pathData.length-1][0], pathData[pathData.length-1][1]);
            rectF = new RectF();
            path.computeBounds(rectF, true);
        } catch (Exception e){
            e.printStackTrace();
            defaultPath();
        }
    }

    private void defaultPath(){
        path = new Path();
        path.addCircle(50, 50, 50, Path.Direction.CW);
        rectF = new RectF();
        path.computeBounds(rectF, true);
    }


    ObjectAnimator objectAnimator;
    public void loadAnimation() {
        if (objectAnimator != null && objectAnimator.isRunning()){
            objectAnimator.end();
        }
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "process", 1f)
                .setDuration(duration);
        objectAnimator.setRepeatMode(ObjectAnimator.RESTART);
        objectAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objectAnimator.start();
    }

    private ValueAnimator clickAnimator;
    public void clickAnimation(){
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        float maxScale = 0.03f;
        float round = 5f;
        layoutParams.width = (int) (width + width*maxScale);
        layoutParams.height = (int) (height + height*maxScale);
        setLayoutParams(layoutParams);
        requestLayout();

        if (clickAnimator != null && clickAnimator.isRunning()) {
            clickAnimator.end();
        }
        clickAnimator = ValueAnimator.ofFloat(0f, 1f);
        clickAnimator.addUpdateListener(animation -> {
            float cosTop = maxScale*(1f-(float)animation.getAnimatedValue());
            float changeScale = (float) (cosTop*Math.cos(round*Math.PI*(float)animation.getAnimatedValue()));
            layoutParams.width = (int) (changeScale*width+width);
            layoutParams.height = (int) (changeScale*height+height);
            MapSkeleton.this.setLayoutParams(layoutParams);
            MapSkeleton.this.requestLayout();
        });
        clickAnimator.setDuration(500);
    }



    public void setProcess(float process) {
        this.process = process;
        invalidate();
    }

    public float getProcess() {
        return process;
    }

    public int getmBackground() {
        return mBackground;
    }

    public void setmBackground(int mBackground) {
        this.mBackground = mBackground;
        invalidate();
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
        invalidate();
    }

    public int getLightColor() {
        return lightColor;
    }

    public void setLightColor(int lightColor) {
        this.lightColor = lightColor;
        invalidate();
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        invalidate();
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        invalidate();
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
        invalidate();
    }

    public String getCityResource() {
        return cityResource;
    }

    public void setCityResource(String cityResource) {
        this.cityResource = cityResource;
        getPath(cityResource);
        requestLayout();
        invalidate();
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
        invalidate();
    }

    public int getCityNameColor() {
        return cityNameColor;
    }

    public void setCityNameColor(int cityNameColor) {
        this.cityNameColor = cityNameColor;
        invalidate();
    }

    public float getCityNameSize() {
        return cityNameSize;
    }

    public void setCityNameSize(float cityNameSize) {
        this.cityNameSize = cityNameSize;
        invalidate();
    }

    public CreatePath getCreatePath() {
        return createPath;
    }

    /**
     * 自定义路径
     * @param createPath 生成路径的接口
     */
    public void setCreatePath(CreatePath createPath) {
        this.createPath = createPath;
        getPathFromData();
        requestLayout();
        invalidate();
    }

    public interface CreatePath{
        float[][] getPath();
    }
}
