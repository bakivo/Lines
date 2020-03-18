package com.example.customgamefield
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Toast
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
        textSize = 65f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("", Typeface.BOLD)
    }
    private enum class Colors(val rgb: Int)   {
        EMPTY(Color.rgb(57, 60, 69)),  // blackish
        TYPE1(Color.rgb(185,164,70)),  // yellowish
        TYPE2(Color.rgb(254,133,118)), // peach
        TYPE3(Color.rgb(167,110,164)), // pinkish
        TYPE4(Color.rgb(123,118,254)), // siren
        TYPE5(Color.rgb(105,165,185)), // bluish
        TYPE6(Color.rgb(109,185,111)), // greenish
        LAST(-1)
    }
    companion object State {
        private const val numNodesInNewBlock = 3
        private const val size = 8
        private const val dimension = size * size
        val nextBlock: Array<Int> = Array(this.numNodesInNewBlock){ Colors.EMPTY.ordinal }
        val fieldState: Array<Int> = Array(this.dimension){ Colors.EMPTY.ordinal }
        var isGamesStarted = false
        var score = 0
    }
    // state vars
    private val goal = 5
    private val negative = -1
    private var selectedCell = negative
    // layout vars
    private var vertOrientation = true

    private val headerArea : RectF = RectF()
    private val newFrame: RectF = RectF()
    private val newXY: PointF = PointF()
    private var dropFrameSize = 0f
    private val dropFrame: RectF = RectF()

    private var fieldSize = 0f
    private val fieldArea: RectF = RectF()
    private var cellSize = 0f
    private val cell: RectF = RectF()

    private val footerArea: RectF = RectF()
    private val scoreXY: PointF = PointF()
    //
    private val cellPadding = 5f
    private val cellRoundness = 15f

    private val text = "Hello toast!"
    private val duration = Toast.LENGTH_SHORT
    val toast = Toast.makeText(context, text, duration)
    // INITIALIZATION
    init {
        toast.setGravity(Gravity.CENTER,0,0)
        isClickable = true
        setBackgroundColor(Color.BLACK);
        context.withStyledAttributes(attrs, R.styleable.fieldView){
            // might be added
        }
    }
    // CONVENIENCE
    private fun isAnySelected() = selectedCell != -1
    private fun isCellEmpty(index: Int) = fieldState[index] == Colors.EMPTY.ordinal
    private fun getNumEmptyCells(): Int = fieldState.count { it == Colors.EMPTY.ordinal }
    private fun toast(s: String){ toast.apply { setText(s) }.show() }
    // EXTENSIONS
    private fun RectF.isClickedOn(x: Float, y: Float) = y in top..bottom && x in left..right
    private fun RectF.setFieldCellXY(index: Int) {
        // field rectangular's top and left coordinates are 0 for calculating current cell's coordinates
        left = fieldArea.left + (index % size) * cellSize + cellPadding
        top =  fieldArea.top +  (index / size) * cellSize + cellPadding
        // bottom and right are calculated simple as
        right = left + cellSize - cellPadding
        bottom = top + cellSize - cellPadding
    }
    private fun RectF.setDropCellXY(index: Int) {
        if (vertOrientation){
            left = dropFrame.left + index * cellSize + cellPadding
            top = dropFrame.top + cellPadding
            right = left + cellSize - cellPadding
            bottom = top + cellSize - cellPadding
        }
        else {
            left = dropFrame.left + cellPadding
            top = dropFrame.top + index * cellSize
            right = left + cellSize - cellPadding
            bottom = top + cellSize - cellPadding
        }
    }
    private fun Array<Int>.moveCell(from: Int, to: Int) {
        this[to] = this[from]
        this[from] = Colors.EMPTY.ordinal
    }
    private fun Array<Int>.getColor(index: Int): Int = when (this[index]) {
            Colors.EMPTY.ordinal -> Colors.EMPTY.rgb
            Colors.TYPE1.ordinal -> Colors.TYPE1.rgb
            Colors.TYPE2.ordinal -> Colors.TYPE2.rgb
            Colors.TYPE3.ordinal -> Colors.TYPE3.rgb
            Colors.TYPE4.ordinal -> Colors.TYPE4.rgb
            Colors.TYPE5.ordinal -> Colors.TYPE5.rgb
            Colors.TYPE6.ordinal -> Colors.TYPE6.rgb
            else -> negative
    }
    // DRAWING
    val fieldIndices: IntRange = fieldState.indices
    val dropIndices: IntRange = nextBlock.indices

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            // draw field based on values of fieldState array------------------------------------------
            for (index in fieldIndices) {
                drawRoundRect(cell.apply { setFieldCellXY(index) }, cellRoundness, cellRoundness,
                    paint.apply { color = fieldState.getColor(index) })
            }
            // draw -- boundary of selected cell -- on the field
            if (isAnySelected()) {
                    drawRoundRect(cell.apply { setFieldCellXY(selectedCell) },
                    cellRoundness, cellRoundness,
                    paint.apply { color = Color.WHITE
                        style = Paint.Style.STROKE})
                paint.style = Paint.Style.FILL
            }
            //draw -- button NEW -- on the header
            drawText("New", newXY.x, newXY.y, paint.apply { color = Color.WHITE })

            if (isGamesStarted)  {
                // draw -- block with next cells to be dropped -- on header
                for (index in dropIndices){
                    drawRoundRect(cell.apply { setDropCellXY(index) },
                        cellRoundness, cellRoundness,
                        paint.apply { color = nextBlock.getColor(index) })
                }
                // draw -- score -- on footer
                drawText("$score", scoreXY.x, scoreXY.y, paint.apply { color = Color.WHITE })
            }
        }
    }

    private fun getIndex(x: Float, y: Float): Int {
        return ((y - fieldArea.top) / cellSize).toInt() * size + ((x - fieldArea.left) / cellSize ).toInt()
    }

    // CLICKING-----------------------------------------------------------------------------------
    private var touchX = 0f
    private var touchY = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        touchX = event.x
        touchY = event.y
        if (event.action == MotionEvent.ACTION_UP) {
            if (fieldArea.isClickedOn(touchX, touchY) && isGamesStarted) {
                processClick(getIndex(touchX, touchY))
                invalidate()
                return true
            }
            if (newFrame.isClickedOn(touchX, touchY)) {
                startGame()
                invalidate()
                return true
            }
        }
        return true
    }
    private fun startGame() {
        score = 0
        selectedCell = -1
        fieldState.fill(Colors.EMPTY.ordinal)
        generateNewBlock()
        for(value in nextBlock) dropCell(value)
        generateNewBlock()
        isGamesStarted = true
        toast("Go!")
    }
    private fun processClick(clicked: Int) {
        if (!isAnySelected()) {
            if (fieldState[clicked] == Colors.EMPTY.ordinal) return
            selectedCell = clicked
            return
        }

        if (!tryMove(selectedCell, clicked)) {
            toast("You shall not pass!")
            return
        }

        if (scoreUp(clicked)) return
        val temp = min(getNumEmptyCells(), numNodesInNewBlock) - 1
        for(index in 0..temp) {
            scoreUp(dropCell(nextBlock[index]))
        }

        if (getNumEmptyCells() == 0) {
            toast("Good game though!")
            gameOver()
            return
        }
        generateNewBlock()

    }

    private fun gameOver() {
        isGamesStarted = false
        fieldState.fill(Colors.EMPTY.ordinal)
    }

    private fun scoreUp(index: Int): Boolean {
        val (left, right, up,down, same) = intArrayOf(-1, 1, -1, 1, 0)
        val color = fieldState[index]
        val line = mutableListOf<Int>()
        val lines = mutableListOf<Int>()
        var x: Int
        var y: Int
        var scoreUp = 0

        fun isWithinBounds(x: Int, y: Int) = x >= 0 && y >= 0 && x < size && y < size
        fun checkDirection(x: Int, y: Int, deltaX: Int, deltaY: Int): Unit	{
            var x = x + deltaX
            var y = y + deltaY
            while (isWithinBounds(x ,y))	{
                var i = y * size + x
                if (fieldState[i] != color) break
                line.add(i)
                x += deltaX
                y += deltaY
            }
        }
        fun checkLine(): Unit{
            if (line.size >= goal - 1) {
                line.forEach{ lines.add(it) }
                line.clear()
            }
        }

        var column = index % size
        var raw = index / size
        //check from index to North-West direction
        checkDirection(column, raw, left, up)
        //check from index to South-East direction
        checkDirection(column, raw, right, down)
        //check line, drop indices to lines if success and clear buffer line
        checkLine()
        //check from index to North-East direction
        checkDirection(column, raw, right, up)
        //check from index to South-West direction
        checkDirection(column, raw, left, down)
        //check line, drop indices to lines if success and clear buffer line
        checkLine()
        //check from index to the left
        checkDirection(column, raw, left, same)
        //check from index to the right
        checkDirection(column, raw, right, same)
        //check line, drop indices to lines if success and clear buffer line
        checkLine()
        //check from index to the up
        checkDirection(column, raw, same, up)
        //check from index to the down
        checkDirection(column, raw, same, down)
        //check line, drop indices to lines if success and clear buffer line
        checkLine()
        if (lines.size > 0){
            scoreUp = lines.size + 1
            score += scoreUp
            // clear cells successfully assembled to lines
            lines.forEach { fieldState[it] = 0 }
            lines.clear()
            fieldState[index] = 0
            toast("+$scoreUp")
            return true
        }
        return false
    }

    private fun tryMove(from: Int, to: Int): Boolean {

        selectedCell = negative
        if (from == to || !findPath(from,to)) return false
        fieldState.moveCell(from, to)
        return true
    }

    // intermediate vars
    private var column = 0      // for checkAdjacentOf()
    private var raw = 0         // ...................
    private var nextIndex = 0   // for checkAdjacentOf()
    private val listToCheck = mutableListOf<Int>()
    private val findings = mutableListOf<Int>()
    private fun findPath(from: Int, to: Int): Boolean {

        fun checkAdjacentOf(index: Int) {
            // calculate coordinates of cell in field Matrix
            column = index % size
            raw = index / size

            // analyze left neighbour
            nextIndex = raw * size + (column - 1)
            if ((column > 0) && isCellEmpty(nextIndex) && !findings.contains(nextIndex)){
                listToCheck.add(nextIndex)
                findings.add(nextIndex)
            }
            // analyze right neighbour
            nextIndex = raw * size + (column + 1)
            if ((column < (size -1)) && isCellEmpty(nextIndex) && !findings.contains(nextIndex)){
                listToCheck.add(nextIndex)
                findings.add(nextIndex)
            }
            // analyze top neighbour
            nextIndex = (raw - 1) * size + column
            if ((raw > 0) && isCellEmpty(nextIndex) && !findings.contains(nextIndex)){
                listToCheck.add(nextIndex)
                findings.add(nextIndex)
            }
            // analyze bottom neighbour
            nextIndex = (raw + 1) * size + column
            if ((raw < (size -1)) && isCellEmpty(nextIndex) && !findings.contains(nextIndex)){
                listToCheck.add(nextIndex)
                findings.add(nextIndex)
            }
            listToCheck.remove(index)
            return
        }

        findings.clear()
        listToCheck.clear()

        listToCheck.add(from)
        while (listToCheck.isNotEmpty())   {
            checkAdjacentOf(listToCheck.first())
        }
        return findings.contains(to)
    }
    // RESIZING------------------------------------------------------------------------------------
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setLayout(w.toFloat(), h.toFloat())
    }
    private fun setLayout(w: Float, h: Float) {
        // (0, 0, w, h) is rectangle to fit fieldArea, headerArea and footerArea in
        // in horizontal orientation headerArea and footerArea placed
        // to the left and to the right side of fieldArea respectively
        fieldSize = min(w, h)
        cellSize = fieldSize / size
        dropFrameSize = numNodesInNewBlock * cellSize
        vertOrientation = w < h
        if (vertOrientation){
            fieldArea.apply {
                left = 0f
                top = (h - fieldSize) / 2
                right = left + fieldSize
                bottom = top + fieldSize
            }
            headerArea.apply {
                left = 0f
                top = 0f
                right = w
                bottom = fieldArea.top
            }
            footerArea.apply {
                left = 0f
                top = fieldArea.bottom
                right = w
                bottom = h
            }
        }
        else {
            fieldArea.apply {
                left = (w - fieldSize) / 2
                top = 0f
                right = left + fieldSize
                bottom = top + fieldSize
            }
            headerArea.apply {
                left = 0f
                top = 0f
                right = fieldArea.left
                bottom = h
            }
            footerArea.apply {
                left = fieldArea.right
                top = 0f
                right = w
                bottom = h
            }
        }
        // set coordinates for objects inside header
        dropFrame.apply {
            if (vertOrientation){
                left = (headerArea.width() - dropFrameSize) / 2
                top = (headerArea.height() - cellSize) / 2
                right  = left + dropFrameSize
                bottom = top + cellSize
            }
            else{
                left = (headerArea.width() - cellSize ) / 2
                top = (headerArea.height() - dropFrameSize) / 2
                right = left + cellSize
                bottom = top + dropFrameSize
            }
        }
        newFrame.apply {
            if (vertOrientation) {
                left = (dropFrame.left - cellSize) / 2
                top = dropFrame.top
                right = left + cellSize
                bottom = dropFrame.bottom
            }
            else {
                left = dropFrame.left
                top = (dropFrame.top - cellSize) / 2
                right = dropFrame.right
                bottom = top + cellSize
            }
        }
        newXY.apply {
            x = newFrame.left + newFrame.width() / 2
            y = newFrame.top + newFrame.height() / 3 * 2
        }
        // set coordinates for score text inside footer
        footerArea.apply {
            scoreXY.x = left + (right - left) / 2
            scoreXY.y = top + (bottom - top) / 2
        }
    }
    // LOGIC -------------------------------------------------------------------------------------


    private fun generateNewBlock() {
        for(i in nextBlock.indices)
            nextBlock[i] = Random.nextInt(Colors.EMPTY.ordinal + 1, Colors.LAST.ordinal)
    }

    private fun dropCell(color: Int): Int{
        val index = fieldState.
                            mapIndexed { index, i -> if (Colors.EMPTY.ordinal == i) index else negative }.
                            filter { it > negative }.random()

        fieldState[index] = color
        return index
    }


}

