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

class FieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintCell = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        //typeface = Typeface.create("", Typeface.BOLD)
    }
    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.WHITE
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
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
        private const val goal = 5
        private const val numNextBlockCells = 3
        private const val size = 8
        private const val dimension = size * size
        val nextBlock: Array<Int> = Array(this.numNextBlockCells){ Colors.EMPTY.ordinal }
        val fieldState: Array<Int> = Array(this.dimension){ Colors.EMPTY.ordinal }
        var isGamesStarted = false
        var score = 0
    }

    // const
    private val negative = -1
    private val cellPadding = 5f
    private val cellRoundness = 15f
    // layout vars
    private var verticalLayout = true
    private val fieldArea: RectF = RectF()
    private val headerArea: RectF = RectF()
    private val footerArea: RectF = RectF()
    private val dropFrame: RectF = RectF()
    private val newFrame: RectF = RectF()
    private val cell: RectF = RectF()
    private val newXY: PointF = PointF()
    private val scoreXY: PointF = PointF()
    private var fieldSize = 0f // rectangle
    private var cellSize = 0f // rectangle
    private var dropFrameSize = 0f // cellSize x numNextDropCells
    private val toast = Toast.makeText(context, "Hello toast!", Toast.LENGTH_SHORT)
    // temporary vars
    private var selectedCell = negative
    private var color = 0
    private val fieldIndices: IntRange = fieldState.indices
    private val dropIndices: IntRange = nextBlock.indices
    private val colorsRange: IntRange = Colors.TYPE1.ordinal until Colors.LAST.ordinal
    private var touchX = 0f
    private var touchY = 0f
    private var column = 0
    private var raw = 0
    private var x = 0
    private var y = 0
    private var nextIndex = 0
    private val listToCheck = mutableListOf<Int>()
    private val findings = mutableListOf<Int>()
    private var randomDropIndex = 0
    private var scoreUp = 0
    private var numLinesBuilt = 0
    private val line = mutableListOf<Int>()
    private val points = mutableListOf<Int>()
    private val rules: List<String> = listOf("Make 5 in line", "to score", "and", "to fight chaos")
    // INITIALIZATION

    init {
        toast.setGravity(Gravity.CENTER,0,0)
        isClickable = true
        setBackgroundColor(Color.BLACK)
        context.withStyledAttributes(attrs, R.styleable.fieldView){
            // might be added
        }
    }

    // CONVENIENCE
    private fun isAnySelected() = selectedCell != negative

    private fun noSelected() = selectedCell == negative

    private fun isCellEmpty(index: Int) = fieldState[index] == Colors.EMPTY.ordinal

    private fun isCellColoured(index: Int) = fieldState[index] != Colors.EMPTY.ordinal

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
        if (verticalLayout){
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

    private fun Array<Int>.moveCell(from: Int, to: Int): Boolean {
        this[to] = this[from]
        this[from] = Colors.EMPTY.ordinal
        return true
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

    // RESIZING------------------------------------------------------------------------------------
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setLayout(w.toFloat(), h.toFloat())
    }

    private fun setLayout(w: Float, h: Float) {
        // (0, 0, w, h) is rectangle to fit field Area, header Area and footer Area in
        // in horizontal orientation header and footer are placed
        // to the left and to the right side of field respectively

        // header contains new button in its own frame and frame for the block of next cells to drop on the field

        // footer contains score

        fieldSize = min(w, h)
        cellSize = fieldSize / size
        dropFrameSize = numNextBlockCells * cellSize
        verticalLayout = w < h
        // first calculate coordinates of the square game field. it's in center regardless orientation
        // based on field coordinates header and footer ones is set
        if (verticalLayout){
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
            if (verticalLayout){
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
            if (verticalLayout) {
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
    // DRAWING -----------------------------------------------------------------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            //draw new button
            drawText("New", newXY.x, newXY.y, paintText)

            if (isGamesStarted)  {
                // draw next block
                for (index in dropIndices){
                    drawRoundRect(cell.apply { setDropCellXY(index) }, cellRoundness, cellRoundness,
                                        paintCell.apply { color = nextBlock.getColor(index) })
                }
                //draw field
                for (index in fieldIndices) {
                    drawRoundRect(cell.apply { setFieldCellXY(index) }, cellRoundness, cellRoundness,
                        paintCell.apply { color = fieldState.getColor(index) })
                }
                // draw selected cell
                if (isAnySelected()) {
                    drawRoundRect(cell.apply { setFieldCellXY(selectedCell) }, cellRoundness, cellRoundness, paintStroke)
                }
                // draw score
                drawText("$score", scoreXY.x, scoreXY.y, paintText)
            }
            else {
                // game is not started
                drawRoundRect(fieldArea,20f,20f, paintCell.apply { color = Colors.EMPTY.rgb })
                for (i in rules.indices){
                    drawText(rules[i],
                        fieldArea.left + fieldArea.width() / 2,
                        fieldArea.top + fieldArea.height() / 3 - paintText.fontMetrics.ascent * i,
                        paintText)
                }
            }
        }
    }
    // CLICKING-----------------------------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        fun getIndex(x: Float, y: Float): Int {
            return size * ((y - fieldArea.top) / cellSize).toInt() + ((x - fieldArea.left) / cellSize).toInt()
        }
        super.onTouchEvent(event)
        touchX = event.x
        touchY = event.y
        if (event.action == MotionEvent.ACTION_UP) {
            if (fieldArea.isClickedOn(touchX, touchY) && isGamesStarted) {
                val i = getIndex(touchX, touchY)
                return if (i in fieldIndices){
                    processClick(i); invalidate(); true
                } else {toast("try again"); false }
            }
            if (newFrame.isClickedOn(touchX, touchY)) {
                startGame(); invalidate(); return true
            }
        }
        return true
    }

    private fun processClick(target: Int): Boolean = when {
        // mark colored cell as selected when clicked on it with no cell selected before
        isCellColoured(target) && noSelected() -> { selectedCell = target; true }
        // just return when clicked on empty cell with no cell selected before
        isCellEmpty(target) && noSelected() -> true
        // return cause not eligible target clicked
        isCellColoured(target) && isAnySelected() -> { selectedCell = negative; toast("yea-nah"); true}
        // attempt to move selected cell to the target
        !tryMove(selectedCell, target).also{ selectedCell = negative } -> { toast("You shall not pass!"); true }
        // return if move succeeded in scoring
        scoreUp(target) -> true
        // if not success...
        else -> {
            //drop cells on random spots; check lines after every drop in series
            for(index in 0 until min(getNumEmptyCells(), numNextBlockCells)) {
                randomDropIndex = dropCell(nextBlock[index])
                    .also { scoreUp(it) }
            }
            // check if empty cells left after dropping to continue game otherwise game over
            if (getNumEmptyCells() > 0) generateNewBlock()
            else gameOver()
            true
        }
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

    private fun gameOver() {
        fun getFinalText(): String {
            return when (score) {
                in 0..10 -> "is it what you are up to?"
                in 11..50 -> "not bad"
                in 51..100 -> "great stuff"
                in 101..150 -> "Wow"
                else -> "OMG"
            }
        }
        toast("$score points - " + getFinalText())
        isGamesStarted = false
        fieldState.fill(Colors.EMPTY.ordinal)
    }

    private fun tryMove(from: Int, to: Int): Boolean = when {
        from == to -> false
        isPathExist(from, to) -> fieldState.run { moveCell(from, to) }
        else -> false
    }

    private fun isPathExist(from: Int, to: Int): Boolean {
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

    private fun scoreUp(index: Int): Boolean {
        val left = -1
        val right = 1
        val up = -1
        val down = 1
        val same = 0
        line.clear()
        points.clear()

        column = index % size
        raw = index / size
        color = fieldState[index]
        numLinesBuilt = 0

        fun isWithinBounds(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < size && y < size

        fun checkDirection(deltaX: Int, deltaY: Int) {
            x = column + deltaX
            y = raw + deltaY
            while (isWithinBounds(x ,y)) {
                (y * size + x).run {// index (aka this) = y * size + x
                    if (fieldState[this] != color) return // exit from fun once color of current index doesn't match
                    else {
                        line.add(this)
                        x += deltaX
                        y += deltaY
                    }
                }
            }
        }

        fun checkLine(): Boolean = when {
            // + 1 in condition as the index being checked is accountable as well but absent in line buffer
            line.size + 1 >= goal -> { line.forEach { points.add(it) }; true }
            else ->  false
        }
        //starting from index as it'd be the center of a compass to check all directions comprising vertical, horizontal and 2 diagonal lines
        //left + right = horizontal line, North-West + South-East = one of two diagonals, etc

        //to North-West
        checkDirection(left, up)
        //to South-East
        checkDirection(right, down)
        //check the line, drop indices to the points buffer if the goal achieved and clear buffer line
        if (checkLine()) numLinesBuilt++
        line.clear()

        //to North-East
        checkDirection(right, up)
        //to South-West
        checkDirection(left, down)
        if (checkLine()) numLinesBuilt++
        line.clear()

        //to the left
        checkDirection(left, same)
        //to the right
        checkDirection(right, same)
        if (checkLine()) numLinesBuilt++
        line.clear()

        //to the up
        checkDirection(same, up)
        //to the down
        checkDirection(same, down)
        if (checkLine()) numLinesBuilt++
        line.clear()

        if (points.size > 0){
            scoreUp = points.size + numLinesBuilt // + because checked index itself is accountable in every line built
            score += scoreUp
            // clear field cells successfully assembled to lines
            points.forEach { fieldState[it] = Colors.EMPTY.ordinal }
            // clear the cell of index itself
            fieldState[index] = Colors.EMPTY.ordinal
            toast("+$scoreUp")
            return true
        }
        return false
    }

    private fun generateNewBlock() {
        for(i in dropIndices)
            nextBlock[i] = colorsRange.random()
    }

    private fun dropCell(color: Int): Int = fieldState.mapIndexed { index, value -> if (value == Colors.EMPTY.ordinal) index else negative }
                                                        .filter { it > negative }
                                                        .random().also { random -> fieldState[random] = color }
    //
}

