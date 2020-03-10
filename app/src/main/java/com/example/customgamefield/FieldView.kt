package com.example.customgamefield
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.graphics.Typeface
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.withStyledAttributes
import kotlin.math.min
import kotlin.random.Random

class FieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // Game vars ------------------------------------------------------------------------------
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = 4f
        typeface = Typeface.create("", Typeface.BOLD)
    }
    private  var vertOrientation = true
    private var color1 = 0
    private var color2 = 0
    private var color3 = 0
    private var touchX = 0f
    private var touchY = 0f
    private var score = 0
    private var selectedCell = -1
    // Header vars -------------------------------------------------------------------Game vars
    private val  headerAreaRect : RectF = RectF(0f,0f,0f,0f)
    private var nextBlock = Array(3){-1}

    // Field vars --------------------------------------------------------------------Header vars
    private val fieldAreaRect: RectF = RectF(0.0f,0.0f,0.0f,0.0f)
    private var fieldSideSize = 0f
    private val numNodesInLine = 9
    private val numNodes = numNodesInLine * numNodesInLine
    private var nodesState = Array(numNodes){Random.nextInt(1,7)}
    // Footer vars -------------------------------------------------------------------Field vars
    private val  footerAreaRect : RectF = RectF(0f,0f,0f,0f)
    // Cell vars --------------------------------------------------------------------Footer vars
    enum class CELL(val value: Float){
        ROUNDNESS(15f),
        PADDING(5f)}
    private var cellSideSize = 0f
    private val newCell: RectF = RectF(0.0f,0.0f,0.0f,0.0f)
    private enum class COLORS(val v: Int){
        EMPTY(Color.LTGRAY),                            // greish
        TYPE1(Color.rgb(255,255,72)),  // yellowish
        TYPE2(Color.rgb(254,133,118)), // peach
        TYPE3(Color.rgb(254,140,228)), // pinkish
        TYPE4(Color.rgb(123,118,254)), // siren
        TYPE5(Color.rgb(35,251,254)),  // tiffany
        TYPE6(Color.rgb(109,254,108)), // greenish

    }
    // --------------------------------------------------------------------------------Cell vars
    init {
        isClickable = true
        setBackgroundColor(Color.BLACK);

        context.withStyledAttributes(attrs, R.styleable.fieldView){
            color1 =getColor(R.styleable.fieldView_nodeColor1,0)
            color2 =getColor(R.styleable.fieldView_nodeColor2,0)
            color3 =getColor(R.styleable.fieldView_nodeColor3,0)
        }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.i("FieldView", "Drawing started")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawRect(fieldAreaRect, paint)
        paint.style = Paint.Style.FILL

        for (index in nodesState.indices){
            setCellColor(index) // update Paint object fill color
            newCell.setNewCellCoordinates(index)
            canvas.drawRoundRect(newCell, CELL.ROUNDNESS.value, CELL.ROUNDNESS.value, paint)
        }
        // highlight selected cell
        if (selectedCell != -1){
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            newCell.setNewCellCoordinates(selectedCell)
            canvas.drawRoundRect(newCell, 0f, 0f, paint)
            paint.style = Paint.Style.FILL
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        vertOrientation = w < h
        fieldSideSize = min(w, h).toFloat()
        cellSideSize = fieldSideSize / numNodesInLine
        setLayout (w.toFloat(), h.toFloat())
    }

    private fun setLayout(w: Float, h: Float) {
        if (vertOrientation){
            fieldAreaRect.left = 0f
            fieldAreaRect.top = (h - fieldSideSize) / 2
            fieldAreaRect.bottom = fieldAreaRect.top + fieldSideSize
            fieldAreaRect.right = fieldAreaRect.left + fieldSideSize
            headerAreaRect.right = w
            headerAreaRect.bottom = fieldAreaRect.top
            footerAreaRect.left = 0f
            footerAreaRect.top = fieldAreaRect.bottom
        }
        else {
            fieldAreaRect.left = (w - fieldSideSize) / 2
            fieldAreaRect.top = 0f
            fieldAreaRect.bottom = fieldAreaRect.top + fieldSideSize
            fieldAreaRect.right = fieldAreaRect.left + fieldSideSize
            headerAreaRect.right = fieldAreaRect.left
            headerAreaRect.bottom = h
            footerAreaRect.left = fieldAreaRect.right
            footerAreaRect.top = 0f
        }
        headerAreaRect.left = 0f
        headerAreaRect.top = 0f
        footerAreaRect.right = w
        footerAreaRect.bottom = h
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        touchX = event.x
        touchY = event.y
        selectedCell = getIndex(event.x,event.y)
        Log.i("FieldView", "x:= + $touchX, y:= $touchY, ------------------ at pos = $selectedCell")
        if (selectedCell > numNodes - 1) {
            Log.i("FieldView", "click out of field")
            return false
        }
        when (event.action){
            MotionEvent.ACTION_UP -> performClick()
        }
        return true

    }

    override fun performClick(): Boolean {
        if (super.performClick()) return true
        // Redraw the view.
        invalidate()
        return true
    }

    private fun RectF.setNewCellCoordinates(num: Int) {
        top = fieldAreaRect.top + (num / numNodesInLine) * cellSideSize + CELL.PADDING.value      //y
        left = fieldAreaRect.left + (num % numNodesInLine) * cellSideSize + CELL.PADDING.value     //x
        bottom = cellSideSize + top - CELL.PADDING.value
        right = cellSideSize + left - CELL.PADDING.value
    }
    private fun RectF.defineFieldSquare(w:Int, h:Int){
        top = 0f      //y
        left = 0f     //x
        bottom = min(w,h).toFloat()
        right = bottom
    }
    private fun getIndex(x: Float, y: Float): Int {
        return ((y - fieldAreaRect.top) / cellSideSize).toInt() * numNodesInLine + ((x - fieldAreaRect.left) / cellSideSize ).toInt()
    }
    private fun setCellColor (index: Int){
        paint.color = when(nodesState[index]) {
            0 -> COLORS.EMPTY.v
            1->  COLORS.TYPE1.v
            2 -> COLORS.TYPE2.v
            3 -> COLORS.TYPE3.v
            4 -> COLORS.TYPE4.v
            5 -> COLORS.TYPE5.v
            6 -> COLORS.TYPE6.v
            else -> Color.DKGRAY
        }
    }

}
