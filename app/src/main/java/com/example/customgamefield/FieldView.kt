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
        textSize = 100f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("", Typeface.BOLD)
    }
    private var vertOrientation = true
    private var color1 = 0
    private var color2 = 0
    private var color3 = 0
    private var touchX = 0f
    private var touchY = 0f
    private var selectedCell = -1
    private var isGamesStarted = false
    // Header vars -------------------------------------------------------------------Game vars
    private val headerAreaRect : RectF = RectF(0f,0f,0f,0f)
    private val numNodesInNewBlock = 3
    private var nextBlock = Array(numNodesInNewBlock){COLORS.EMPTY.ordinal}

    // Field vars ---------------------------------------------------------------
    private val fieldAreaRect: RectF = RectF(0.0f,0.0f,0.0f,0.0f)
    private var fieldSideSize = 0f
    private val numNodesInLine = 9
    private val numNodes = numNodesInLine * numNodesInLine
    private var nodesState = Array(numNodes){COLORS.TYPE2.ordinal}
    // Footer vars -------------------------------------------------------------
    private val  footerAreaRect : RectF = RectF(0f,0f,0f,0f)
    private var score = 0
    // Cell vars ---------------------------------------------------------------
    private val newCell: RectF = RectF(0.0f,0.0f,0.0f,0.0f)
    private enum class CELL(val value: Float){
        ROUNDNESS(15f),
        PADDING(5f)}
    private var cellSideSize = 0f
    private enum class COLORS(val v: Int){
        EMPTY(Color.LTGRAY),                            // greish
        TYPE1(Color.rgb(255,255,72)),  // yellowish
        TYPE2(Color.rgb(254,133,118)), // peach
        TYPE3(Color.rgb(254,140,228)), // pinkish
        TYPE4(Color.rgb(123,118,254)), // siren
        TYPE5(Color.rgb(35,251,254)),  // tiffany
        TYPE6(Color.rgb(109,254,108)), // greenish

    }
    // INITIALIZATION-----------------------------------------------------------
    init {
        Log.i("FieldView", "initialization")
        isClickable = true
        setBackgroundColor(Color.BLACK);
        context.withStyledAttributes(attrs, R.styleable.fieldView){
            color1 =getColor(R.styleable.fieldView_nodeColor1,0)
            color2 =getColor(R.styleable.fieldView_nodeColor2,0)
            color3 =getColor(R.styleable.fieldView_nodeColor3,0)
        }
    }

    // DRAWING
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.i("FieldView", "Drawing started")
        // draw next block on header ------------------------------------------------------------
        paint.color = Color.MAGENTA
        canvas.drawText("Hit", 60f,
                                    headerAreaRect.bottom / 2, paint)
        if(isGamesStarted){
            for (i in nextBlock.indices){
                setColorNextCell(i)
                newCell.setNewDropCellCoordinate(i)
                canvas.drawRoundRect(newCell, CELL.ROUNDNESS.value,
                                                CELL.ROUNDNESS.value, paint)
            }
        }
        // draw field---------------------------------------------------------------------------
        for (index in nodesState.indices){
            setCellColor(index) // update Paint object fill color
            newCell.setNewCellCoordinates(index)
            canvas.drawRoundRect(newCell, CELL.ROUNDNESS.value, CELL.ROUNDNESS.value, paint)
        }
        // highlight selected cell
        if (selectedCell != -1){
            paint.style = Paint.Style.STROKE
            paint.color = Color.WHITE
            newCell.setNewCellCoordinates(selectedCell)
            canvas.drawRoundRect(newCell, CELL.ROUNDNESS.value, CELL.ROUNDNESS.value, paint)
            paint.style = Paint.Style.FILL
        }
        // draw footer with score--------------------------------------------------------------
        if(isGamesStarted)  {
            paint.color = Color.WHITE
            canvas.drawText("$score",
                            (footerAreaRect.right - footerAreaRect.left) / 2,
                            fieldAreaRect.bottom + (footerAreaRect.bottom - footerAreaRect.top) / 2,
                                paint)
        }
    }
    private fun setColorNextCell(i: Int) {
        paint.color = when(nextBlock[i]) {
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
    private fun setCellColor (i: Int){
        paint.color = when(nodesState[i]) {
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
    private fun getIndex(x: Float, y: Float): Int {
        return ((y - fieldAreaRect.top) / cellSideSize).toInt() * numNodesInLine + ((x - fieldAreaRect.left) / cellSideSize ).toInt()
    }

    // CLICKING-----------------------------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.i("FieldView", "clicked")
        super.onTouchEvent(event)
        touchX = event.x
        touchY = event.y
        when (event.action){
            MotionEvent.ACTION_DOWN -> performClick()
        }
        return true
    }
    override fun performClick(): Boolean {
        super.performClick()
        if (fieldAreaRect.isClickedOnField(touchX, touchY)){
            if (!isGamesStarted) return true
            selectedCell = getIndex(touchX,touchY)
            Log.i("FieldView", "pos = $selectedCell")
            for(value in nextBlock) dropCell(value)
            generateNewBlock()
        }


        if (headerAreaRect.isClickedOnField(touchX, touchY)){
            Log.i("FieldView", "game started")
            startGame()
        }
        if (footerAreaRect.isClickedOnField(touchX, touchY)) {
            Log.i("FieldView", "clicked on footer")
        }

        // Redraw the view.
        invalidate()
        return true
    }

    // RESIZING------------------------------------------------------------------------------------
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

    // EXTENSIONS---------------------------------------------------------------------------------
    private fun RectF.isClickedOnField (x: Float, y: Float) = y in top..bottom && x in left..right
    private fun RectF.setNewCellCoordinates(num: Int) {
        top = fieldAreaRect.top + (num / numNodesInLine) * cellSideSize + CELL.PADDING.value      //y
        left = fieldAreaRect.left + (num % numNodesInLine) * cellSideSize + CELL.PADDING.value     //x
        bottom = cellSideSize + top - CELL.PADDING.value
        right = cellSideSize + left - CELL.PADDING.value
    }
    private fun RectF.setNewDropCellCoordinate(i: Int) {
        if (vertOrientation){
            left = headerAreaRect.right / 2 - cellSideSize * 3 / 2 + cellSideSize * i
            top = headerAreaRect.bottom / 2 - cellSideSize / 2
            right = left + cellSideSize
            bottom = top + cellSideSize
        }
        else {
            left = headerAreaRect.right / 2 - cellSideSize / 2
            top = headerAreaRect.bottom / 2  - cellSideSize * 3 / 2 + cellSideSize * i
            right = left + cellSideSize
            bottom = top + cellSideSize
        }
    }

    // LOGIC -------------------------------------------------------------------------------------
    private fun startGame() {
        resetState()
    }

    private fun resetState() {
        score = 0
        selectedCell = -1
        for(i in nodesState.indices) nodesState[i] = COLORS.EMPTY.ordinal
        generateNewBlock()
        for(value in nextBlock) dropCell(value)
        generateNewBlock()
        isGamesStarted = true
    }

    private fun getNumEmptyCells(): Int{
        var res = 0
        for (i in nodesState) if (COLORS.EMPTY.ordinal == i) res++
        Log.i("FieldView","empties = $res")
        return res
    }

    private fun generateNewBlock() {
        for(i in nextBlock.indices) nextBlock[i] = Random.nextInt(1,7)
    }

    private fun dropCell(value: Int) {
        var randomIndex = Random.nextInt(0,getNumEmptyCells() - 1)
        //Log.i("FieldView","randomIndex to drop at = $randomIndex")
        //Log.i("FieldView","value to drop = $value")
        var curRandomIndex = 0
        for(i in nodesState.indices){
            if (nodesState[i] == COLORS.EMPTY.ordinal){
                if( randomIndex == curRandomIndex){
                    nodesState[i] = value
                    return
                }
                curRandomIndex++
            }
        }
    }
}

