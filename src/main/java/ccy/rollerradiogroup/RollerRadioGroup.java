package ccy.rollerradiogroup;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ccy on 2017-11-29.
 * 横向滚动单选控件<br/>
 * 可用属性：<br/>
 * normal_color 未选中文字颜色<br/>
 * selected_color 选中文字颜色<br/>
 * normal_size  未选中文字大小<br/>
 * selected_size  选中文字大小<br/>
 * text_padding 文字间距<br/>
 * shader_color 阴影颜色，如果不设置，则与background相同，如果background不是一个颜色值,则默认白色<br/>
 * show_edge_line 是否显示上下边框的线条
 * 用法：<br/>
 * 通过{@link RollerRadioGroup#setData(List, int)}、{@link RollerRadioGroup#setData(List)}传入数据<br/>
 * 通过其他setter方法进行配置
 */

public class RollerRadioGroup extends View {
    //默认
    private static final int NORMAL_COLOR = 0XFF000000;
    private static final int SELECTED_COLOR = 0XFF000000;
    private final float NORMAL_SIZE = sp2px(14);
    //    private final float SELECT_SIZE = 1.3f * NORMAL_SIZE;
    private final float DEFAULT_PADDING = dp2pxf(10);

    //属性
    private int normalColor; //未选中字体颜色
    private int selectedColor; //选中字体颜色
    private float normalSize; //未选中字体大小
    private float selectedSize;//选中字体大小
    private float textPadding;//相邻文字间距
    //    private boolean clipEdge; //true则因超出边界而显示不完整的文字不画. 备注：懒的写 算了
    private int shaderColor; //阴影颜色
    private boolean showEdgeLine; //是否显示上下边框的线条

    //数据
    private List<String> texts = new ArrayList<>();
    private int selectedId = -1;
    private List<Rect> textsRects = new ArrayList<>(); //记录各项文字rect
    private List<Float> textsCenterX = new ArrayList<>();  //记录各项文字中心x坐标
    private float contentWidth; //内容的实际宽度
    private OnRollerListener listener;

    //绘图
    private Scroller scroller;
    private VelocityTracker velocityTracker;
    private ViewConfiguration viewConfiguration;
    private Paint norPaint;
    private Paint selPaint;
    private Paint shaderPaint;
    private Paint.FontMetrics norFont;
    private Paint.FontMetrics selFont;
    private boolean isTouching;


    public RollerRadioGroup(Context context) {
        this(context, null);
    }

    public RollerRadioGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RollerRadioGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        scroller = new Scroller(context);
        viewConfiguration = ViewConfiguration.get(context);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RollerRadioGroup);
        normalColor = ta.getColor(R.styleable.RollerRadioGroup_normal_color, NORMAL_COLOR);
        selectedColor = ta.getColor(R.styleable.RollerRadioGroup_selected_color, SELECTED_COLOR);
        normalSize = ta.getDimension(R.styleable.RollerRadioGroup_normal_size, NORMAL_SIZE);
        selectedSize = ta.getDimension(R.styleable.RollerRadioGroup_selected_size, 1.3f * normalSize);
//        clipEdge = ta.getBoolean(R.styleable.RollerRadioGroup_clip_edge, false);
        textPadding = ta.getDimension(R.styleable.RollerRadioGroup_text_padding, DEFAULT_PADDING);
        shaderColor = ta.getColor(R.styleable.RollerRadioGroup_shader_color, getBackgroundColor());
        showEdgeLine = ta.getBoolean(R.styleable.RollerRadioGroup_show_edge_line, false);
        ta.recycle();

        initPaint();


    }

    private void initPaint() {
        norPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        norPaint.setColor(normalColor);
        norPaint.setTextSize(normalSize);
        norPaint.setTextAlign(Paint.Align.CENTER);
        norFont = norPaint.getFontMetrics();

        selPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selPaint.setColor(selectedColor);
        selPaint.setTextSize(selectedSize);
        selPaint.setTextAlign(Paint.Align.CENTER);
        selPaint.setStrokeWidth(dp2pxf(1));
        selFont = selPaint.getFontMetrics();

        shaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ws = MeasureSpec.getSize(widthMeasureSpec);
        int wm = MeasureSpec.getMode(widthMeasureSpec);
        int hs = MeasureSpec.getSize(heightMeasureSpec);
        int hm = MeasureSpec.getMode(heightMeasureSpec);

        if (wm != MeasureSpec.EXACTLY) {
            ws = dp2px(150);
        }
        if (hm != MeasureSpec.EXACTLY) {
            hs = (int) (Math.max(selectedSize,normalSize) + dp2px(20));
        }

        setMeasuredDimension(ws, hs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTexts(canvas);

        if (showEdgeLine) {
            drawEdgeLine(canvas);
        }

        drawShader(canvas);


    }

    /**
     * 绘制上下边框线条
     */
    private void drawEdgeLine(Canvas canvas) {
        canvas.save();
        canvas.drawLine(-getMeasuredWidth(), 0, contentWidth + getMeasuredWidth(), 0, selPaint);
        canvas.drawLine(-getMeasuredWidth(), getMeasuredHeight(), contentWidth + getMeasuredWidth(), getMeasuredHeight(), selPaint);
        canvas.restore();
    }


    /**
     * 绘制文字
     */
    private void drawTexts(Canvas canvas) {
        canvas.save();
        Paint.FontMetrics f = selectedSize > normalSize ? selFont : norFont;
        float centerY = getMeasuredHeight() / 2.0f - (f.ascent + f.descent) / 2;
        for (int i = 0; i < texts.size(); i++) {
            canvas.drawText(texts.get(i), textsCenterX.get(i), centerY, i == selectedId ? selPaint : norPaint);
        }
        canvas.restore();
    }

    /**
     * 绘制边界阴影
     */
    private void drawShader(Canvas canvas) {
        canvas.save();
        float rectWidth = getMeasuredWidth() * 0.2f;
//        int startColor = getBackgroundColor() & 0X00FFFFFF; //将ARGB的A置为00，其他不变
//        int stopColor = getBackgroundColor();
//        if((stopColor & 0XFF000000)  == 0){  //如果用户只给了RGB 则将A置为FF
//            stopColor = getBackgroundColor() | 0XFF000000;
//        }
        int startColor = shaderColor & 0X00FFFFFF; //将ARGB(也可能是RGB）的A置为00，其他不变
        int stopColor = shaderColor;
        if ((stopColor & 0XFF000000) == 0) {  //如果用户只给了RGB 则手动给A设为FF
            stopColor = shaderColor | 0XFF000000;
        }
        LinearGradient leftShader = new LinearGradient(getScrollX(),
                0,
                getScrollX() + rectWidth,
                0,
                stopColor,
                startColor,
                Shader.TileMode.CLAMP);
        shaderPaint.setShader(leftShader);
        canvas.drawRect(getScrollX(),
                getScrollY(),
                getScrollX() + rectWidth,
                getScrollY() + getMeasuredHeight(),
                shaderPaint);


        LinearGradient rightShader = new LinearGradient(getScrollX() + getMeasuredWidth() - rectWidth,
                0,
                getScrollX() + getMeasuredWidth(),
                0,
                startColor,
                stopColor,
                Shader.TileMode.CLAMP);
        shaderPaint.setShader(rightShader);
        canvas.drawRect(getScrollX() + getMeasuredWidth() - rectWidth,
                getScrollY(),
                getScrollX() + getMeasuredWidth(),
                getScrollY() + getMeasuredHeight(),
                shaderPaint);
        canvas.restore();
    }


    private int getBackgroundColor() {
        Drawable drawable = getBackground();
        if (drawable instanceof ColorDrawable) {
            return ((ColorDrawable) drawable).getColor();
        }
        return Color.WHITE;
    }


    //onTouch相关变量
    private float lastX = 0;
    private float x = 0;
    private float beginScrollX = 0;  //触摸事件开始时的scrollX
    private boolean isClickEvent; //判定是否属于点击事件

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (contentWidth <= getMeasuredWidth()) {  //如果内容长度没有超出控件长度，无需支持滑动， 直接结束
            return true;
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouching = true;
                if (!scroller.isFinished()) {  //fling还没结束
                    scroller.abortAnimation();
                }
                beginScrollX = getScrollX();
                lastX = event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                int deltaX = (int) (lastX - x);
                if (Math.abs(deltaX) < viewConfiguration.getScaledTouchSlop()) {
                    isClickEvent = true;
                } else {
                    isClickEvent = false;
                    //备注：不用考虑越界修复了，不然开头几个和末尾几个不容易选到
                    scrollTo((int) (beginScrollX + deltaX), 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                x = event.getX();
                if (isClickEvent) {  //判定为点击事件
                    setSelectedId(calculateNewSelectedId(beginScrollX + x));
                } else {  //判定为滑动或抛动事件
                    velocityTracker.computeCurrentVelocity(1000);  //计算1秒内滑动过多少像素
                    int xVelocity = (int) velocityTracker.getXVelocity();
                    if (Math.abs(xVelocity) > viewConfiguration.getScaledMinimumFlingVelocity()) {  //滑动速度可被判定为抛动
                        scroller.fling(getScrollX(),
                                0,
                                -xVelocity,
                                0,
                                -getMeasuredWidth() / 2, //允许向左越界半个控件宽
                                (int) contentWidth - getMeasuredWidth() / 2, //允许向右越界半个控件宽
                                0,
                                0);
                        invalidate();
                    } else { //滑动速度不被认定为抛动
                        setSelectedId(calculateNewSelectedId());
                    }
                }

                isTouching = false;
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 根据当前滑动距离和控件位置找到离控件中心最近的item，返回它的id
     */
    private int calculateNewSelectedId() {
        float viewCenterX = getScrollX() + getMeasuredWidth() / 2.0f; //控件中心x
        return calculateNewSelectedId(viewCenterX);
    }

    /**
     * 找到离参数viewCenterX最近的一个item，返回它的id
     */
    private int calculateNewSelectedId(float viewCenterX) {
        int findSelectedId = 0;  //找到离x最近的item
        float lastDeltaX = 0;
        for (int i = 0; i < textsCenterX.size(); i++) {
            if (i == 0) {
                lastDeltaX = Math.abs(textsCenterX.get(i) - viewCenterX);
                findSelectedId = i;
                continue;
            }

            if (Math.abs(textsCenterX.get(i) - viewCenterX) < lastDeltaX) {
                findSelectedId = i;
            }
            lastDeltaX = Math.abs(textsCenterX.get(i) - viewCenterX);
        }
        return findSelectedId;
    }


    /**
     * 尝试将当前选中项滚动到控件居中的位置。
     */
    private void tryScrollToSelectedItem() {
        //计算位移
        float deltaX = textsCenterX.get(selectedId) - (getScrollX() + getMeasuredWidth() / 2.0f);
        if (deltaX >= -1 && deltaX <= 1) {  //容差 0±1，因为int/float转型过程可能导致结果无法正好为0，
            return;
        }
        scroller.startScroll(getScrollX(), getScrollY(), (int) deltaX, getScrollY());
        invalidate();
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            invalidate();
        } else {
            if (!isTouching) {  //滑动结束且手指已抬起
                setSelectedId(calculateNewSelectedId());
            }
        }
    }

    /**
     * 避免onDraw过多计算和实例，在这计算好绘制所需的数据
     */
    private void initData() {

        //1、记录各项文字的边界
        textsRects.clear();
        //这里全部按较大的画笔记录数据，
        //这样在选中的文字放大时两边文字不会因挤压而改变内容的x坐标
        Paint p = selectedSize > normalSize ? selPaint : norPaint;
        for (int i = 0; i < texts.size(); i++) {
            Rect rect = new Rect();
            p.getTextBounds(texts.get(i), 0, texts.get(i).length(), rect);
            textsRects.add(rect);
        }

        //2、记录各项文字的中心x坐标
        textsCenterX.clear();
        float sumX = 0;
        for (int i = 0; i < texts.size(); i++) {
            sumX += textPadding + textsRects.get(i).width();
            textsCenterX.add(sumX - textsRects.get(i).width() / 2.0f);
        }
        contentWidth = sumX + textPadding;
    }


    //以下为对外公开的设置方法

    /**
     * 设置数据
     */
    public void setData(List<String> texts, final int selectedId) {
        this.texts = texts;
//        this.selectedId = selectedId;
        initData();
        requestLayout();

        post(new Runnable() {
            @Override
            public void run() {
                setSelectedId(selectedId);
            }
        });
    }

    /**
     * 设置数据
     */
    public void setData(List<String> texts) {
        this.texts = texts;
        initData();
        requestLayout();
    }

    public List<String> getData() {
        return texts;
    }

    /**
     * 设置当前选中项
     *
     * @param selectedId 数据对应的位置
     */
    public boolean setSelectedId(int selectedId) {
        return setSelectedId(selectedId, false);
    }

    /**
     * 设置当前选中项
     *
     * @param itemName 数据匹配的内容
     */
    public boolean setSelectedId(String itemName) {
        return setSelectedId(itemName, false);
    }

    /**
     * 设置当前选中项
     *
     * @param selectedId 数据对应的位置
     * @param noListener true则不触发监听回调
     */
    public boolean setSelectedId(int selectedId, boolean noListener) {
        if (texts != null && selectedId >= 0 && selectedId < texts.size()) {  //不要在这里判断this.selectedId != selectedId
            if (!noListener && listener != null && this.selectedId != selectedId) {
                listener.onItemSelected(this, selectedId, this.selectedId);
            }
            this.selectedId = selectedId;
            tryScrollToSelectedItem();
            return true;
        }
        return false;
    }

    /**
     * 设置当前选中项
     *
     * @param itemName   数据匹配的内容
     * @param noListener true则不触发监听回调
     */
    public boolean setSelectedId(String itemName, boolean noListener) {
        if (itemName != null) {
            int id = texts.indexOf(itemName);
            if (id != -1) {   //不要在这里判断this.selectedId != id
                if (!noListener && listener != null && this.selectedId != id) {
                    listener.onItemSelected(this, id, this.selectedId);
                }
                this.selectedId = id;
                tryScrollToSelectedItem();
                return true;
            }
        }
        return false;
    }

    public int getSelectedId() {
        return selectedId;
    }

    public void setOnRollerListener(OnRollerListener listener) {
        this.listener = listener;
    }


    public int getNormalColor() {
        return normalColor;
    }

    /**
     * @param normalColor 需要是ARGB
     */
    public void setNormalColor(int normalColor) {
        this.normalColor = normalColor;
        initPaint();
        invalidate();
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    /**
     * @param selectedColor 需要是ARGB
     */
    public void setSelectedColor(int selectedColor) {
        this.selectedColor = selectedColor;
        initPaint();
        invalidate();
    }

    public float getNormalSize() {
        return normalSize;
    }

    public void setNormalSize(float normalSize) {
        this.normalSize = normalSize;
        initPaint();
        initData();
        requestLayout();
    }

    public float getSelectedSize() {
        return selectedSize;
    }

    public void setSelectedSize(float selectedSize) {
        this.selectedSize = selectedSize;
        initPaint();
        initData();
        requestLayout();
    }

    public float getTextPadding() {
        return textPadding;
    }

    public void setTextPadding(float textPadding) {
        this.textPadding = textPadding;
        initData();
        requestLayout();
    }

    public int getShaderColor() {
        return shaderColor;
    }

    public void setShaderColor(int shaderColor) {
        this.shaderColor = shaderColor;
        invalidate();
    }

    public boolean isShowEdgeLine() {
        return showEdgeLine;
    }

    public void setShowEdgeLine(boolean showEdgeLine) {
        this.showEdgeLine = showEdgeLine;
        invalidate();
    }


    public interface OnRollerListener {
        /**
         * item选中回调
         *
         * @param selectedId     新的选中item位置
         * @param lastSelectedId 上一个选中的item位置
         */
        void onItemSelected(RollerRadioGroup v, int selectedId, int lastSelectedId);

    }


    //工具相关

    private float dp2pxf(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float sp2px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }


}
