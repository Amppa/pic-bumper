package com.picbumper.ui.home.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.picbumper.R
import com.picbumper.domain.model.ImageItem
import com.picbumper.ui.home.GridEntry

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_PHOTO = 1

@Composable
fun MemePhotoGrid(
    gridEntries: List<GridEntry>,
    checkedItemUris: Set<Uri>,
    isMultiSelectMode: Boolean,
    thumbnailSize: Int,
    onClick: (ImageItem) -> Unit,
    onLongClick: (ImageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()

    val adapter = remember {
        MemeGridAdapter(
            primaryColorArgb = primaryColorArgb,
            thumbnailSize = thumbnailSize,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }

    adapter.updateData(
        entries = gridEntries,
        checkedItemUris = checkedItemUris,
        isMultiSelect = isMultiSelectMode,
        thumbSize = thumbnailSize,
        primaryColor = primaryColorArgb,
        onClick = onClick,
        onLongClick = onLongClick
    )

    AndroidView(
        factory = { ctx ->
            RecyclerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setHasFixedSize(true)
                setItemViewCacheSize(20)
                setPadding(dpToPx(ctx, 2), dpToPx(ctx, 2), dpToPx(ctx, 2), dpToPx(ctx, 96))
                clipToPadding = false

                val gridLayoutManager = GridLayoutManager(ctx, 4).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (adapter.getItemViewType(position)) {
                                VIEW_TYPE_HEADER -> spanCount
                                else -> 1
                            }
                        }
                    }
                }
                layoutManager = gridLayoutManager
                this.adapter = adapter
            }
        },
        update = { recyclerView ->
            val layoutManager = recyclerView.layoutManager as? GridLayoutManager
            if (layoutManager != null) {
                val displayWidth = recyclerView.resources.displayMetrics.widthPixels
                val minColumnWidth = dpToPx(recyclerView.context, 105)
                val calculatedSpanCount = (displayWidth / minColumnWidth).coerceAtLeast(3)
                if (layoutManager.spanCount != calculatedSpanCount) {
                    layoutManager.spanCount = calculatedSpanCount
                }
            }
        },
        modifier = modifier
    )
}

private class MemeGridAdapter(
    private var primaryColorArgb: Int,
    private var thumbnailSize: Int,
    private var onClick: (ImageItem) -> Unit,
    private var onLongClick: (ImageItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var entries: List<GridEntry> = emptyList()
    private var checkedUris: Set<Uri> = emptySet()
    private var isMultiSelect: Boolean = false

    fun updateData(
        entries: List<GridEntry>,
        checkedItemUris: Set<Uri>,
        isMultiSelect: Boolean,
        thumbSize: Int,
        primaryColor: Int,
        onClick: (ImageItem) -> Unit,
        onLongClick: (ImageItem) -> Unit
    ) {
        this.entries = entries
        this.checkedUris = checkedItemUris
        this.isMultiSelect = isMultiSelect
        this.thumbnailSize = thumbSize
        this.primaryColorArgb = primaryColor
        this.onClick = onClick
        this.onLongClick = onLongClick
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = entries.size

    override fun getItemViewType(position: Int): Int {
        return when (entries[position]) {
            is GridEntry.Header -> VIEW_TYPE_HEADER
            is GridEntry.Photo -> VIEW_TYPE_PHOTO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val ctx = parent.context
        return if (viewType == VIEW_TYPE_HEADER) {
            val headerView = createHeaderView(ctx)
            HeaderViewHolder(headerView)
        } else {
            val photoView = createPhotoTileView(ctx)
            PhotoViewHolder(photoView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val entry = entries[position]) {
            is GridEntry.Header -> (holder as HeaderViewHolder).bind(entry)
            is GridEntry.Photo -> (holder as PhotoViewHolder).bind(
                entry.item,
                checkedUris.contains(entry.item.uri),
                isMultiSelect,
                thumbnailSize,
                primaryColorArgb,
                onClick,
                onLongClick
            )
        }
    }

    private fun createHeaderView(ctx: Context): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad = dpToPx(ctx, 8)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val line1 = View(ctx).apply {
                setBackgroundColor(AndroidColor.parseColor("#2D2D2D"))
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(ctx, 1), 1f)
            }
            val titleText = TextView(ctx).apply {
                id = R.id.header_title
                setTextColor(primaryColorArgb)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                val textPad = dpToPx(ctx, 12)
                setPadding(textPad, 0, textPad, 0)
            }
            val line2 = View(ctx).apply {
                setBackgroundColor(AndroidColor.parseColor("#2D2D2D"))
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(ctx, 1), 1f)
            }

            addView(line1)
            addView(titleText)
            addView(line2)
        }
    }

    private fun createPhotoTileView(ctx: Context): SquareFrameLayout {
        val container = SquareFrameLayout(ctx).apply {
            val margin = dpToPx(ctx, 2)
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(margin, margin, margin, margin)
            layoutParams = params
            setBackgroundColor(AndroidColor.parseColor("#242424"))
        }

        val imageView = ImageView(ctx).apply {
            id = R.id.tile_image
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        container.addView(imageView)

        // Overlay View for 3.dp Selection Border (0 SaveLayer overdraw)
        val borderOverlay = View(ctx).apply {
            id = R.id.tile_border
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        container.addView(borderOverlay)

        // Badge Container (Top-End)
        val badgeView = TextView(ctx).apply {
            id = R.id.tile_badge
            val badgeSize = dpToPx(ctx, 22)
            val params = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                gravity = Gravity.TOP or Gravity.END
                val margin6 = dpToPx(ctx, 6)
                setMargins(margin6, margin6, margin6, margin6)
            }
            layoutParams = params
            gravity = Gravity.CENTER
            text = "✓"
            setTextColor(AndroidColor.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            visibility = View.GONE
        }
        container.addView(badgeView)

        return container
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.header_title)
        fun bind(header: GridEntry.Header) {
            titleTextView.text = header.title
        }
    }

    class PhotoViewHolder(val container: SquareFrameLayout) : RecyclerView.ViewHolder(container) {
        private val imageView: ImageView = container.findViewById(R.id.tile_image)
        private val borderOverlay: View = container.findViewById(R.id.tile_border)
        private val badgeView: TextView = container.findViewById(R.id.tile_badge)

        fun bind(
            item: ImageItem,
            isChecked: Boolean,
            isMultiSelect: Boolean,
            thumbnailSize: Int,
            primaryColorArgb: Int,
            onClick: (ImageItem) -> Unit,
            onLongClick: (ImageItem) -> Unit
        ) {
            val ctx = container.context

            imageView.load(item.uri) {
                memoryCacheKey("${item.uriString}_$thumbnailSize")
                diskCacheKey("${item.uriString}_$thumbnailSize")
                size(thumbnailSize)
                scale(Scale.FILL)
                precision(Precision.INEXACT)
                crossfade(false)
            }

            if (isChecked) {
                borderOverlay.visibility = View.VISIBLE
                val borderDrawable = GradientDrawable().apply {
                    setStroke(dpToPx(ctx, 3), primaryColorArgb)
                    setColor(AndroidColor.TRANSPARENT)
                }
                borderOverlay.background = borderDrawable
            } else {
                borderOverlay.visibility = View.GONE
            }

            if (isMultiSelect || isChecked) {
                badgeView.visibility = View.VISIBLE
                val badgeDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (isChecked) {
                        setColor(primaryColorArgb)
                        setStroke(dpToPx(ctx, 1), primaryColorArgb)
                    } else {
                        setColor(AndroidColor.parseColor("#202020"))
                        setStroke(dpToPx(ctx, 1), AndroidColor.parseColor("#666666"))
                    }
                }
                badgeView.background = badgeDrawable
                badgeView.text = if (isChecked) "✓" else ""
            } else {
                badgeView.visibility = View.GONE
            }

            container.setOnClickListener { onClick(item) }
            container.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }
}

private class SquareFrameLayout(context: Context) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}

private fun dpToPx(context: Context, dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}
