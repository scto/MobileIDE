/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.widget.component;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.R;
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent;
import io.github.rosemoe.sora.event.DragSelectStopEvent;
import io.github.rosemoe.sora.event.EditorFocusChangeEvent;
import io.github.rosemoe.sora.event.EditorReleaseEvent;
import io.github.rosemoe.sora.event.EventManager;
import io.github.rosemoe.sora.event.HandleStateChangeEvent;
import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.event.LongPressEvent;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.EditorTouchEventHandler;
import io.github.rosemoe.sora.widget.base.EditorPopupWindow;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * This window will show when selecting text to present text actions.
 *
 * @author Rosemoe
 */
public class EditorTextActionWindow extends EditorPopupWindow implements View.OnClickListener, EditorBuiltinComponent {
    private final static long DELAY = 200;
    private final static long CHECK_FOR_DISMISS_INTERVAL = 100;
    private final CodeEditor editor;
    private final ImageButton selectAllBtn;
    private final ImageButton pasteBtn;
    private final ImageButton copyBtn;
    private final ImageButton cutBtn;
    private final ImageButton longSelectBtn;
    private final View rootView;
    private final EditorTouchEventHandler handler;
    private final EventManager eventManager;
    private long lastScroll;
    private int lastPosition;
    private int lastCause;
    private boolean enabled = true;
    
    // 额外按钮提供者列表
    private final List<ExtraButtonEntry> extraButtonEntries = new ArrayList<>();
    private ViewGroup buttonContainer;

    /**
     * Create a panel for the given editor
     *
     * @param editor Target editor
     */
    public EditorTextActionWindow(CodeEditor editor) {
        super(editor, FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED);
        this.editor = editor;
        handler = editor.getEventHandler();
        eventManager = editor.createSubEventManager();

        // Since popup window does provide decor view, we have to pass null to this method
        @SuppressLint("InflateParams")
        View root = this.rootView = LayoutInflater.from(editor.getContext()).inflate(R.layout.text_compose_panel, null);
        selectAllBtn = root.findViewById(R.id.panel_btn_select_all);
        cutBtn = root.findViewById(R.id.panel_btn_cut);
        copyBtn = root.findViewById(R.id.panel_btn_copy);
        longSelectBtn = root.findViewById(R.id.panel_btn_long_select);
        pasteBtn = root.findViewById(R.id.panel_btn_paste);

        selectAllBtn.setOnClickListener(this);
        cutBtn.setOnClickListener(this);
        copyBtn.setOnClickListener(this);
        pasteBtn.setOnClickListener(this);
        longSelectBtn.setOnClickListener(this);
        
        // 获取按钮容器，用于动态添加额外按钮
        View scrollView = root.findViewById(R.id.panel_hv);
        if (scrollView instanceof ViewGroup) {
            View child = ((ViewGroup) scrollView).getChildAt(0);
            if (child instanceof ViewGroup) {
                buttonContainer = (ViewGroup) child;
            }
        }

        applyColorScheme();
        setContentView(root);
        setSize(0, (int) (this.editor.getDpUnit() * 48));
        getPopup().setAnimationStyle(R.style.text_action_popup_animation);

        subscribeEvents();
    }

    protected void applyColorFilter(ImageButton btn, int color) {
        var drawable = btn.getDrawable();
        if (drawable == null) {
            return;
        }
        btn.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
    }

    protected void applyColorScheme() {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(5 * editor.getDpUnit());
        gd.setColor(editor.getColorScheme().getColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND));
        rootView.setBackground(gd);
        int color = editor.getColorScheme().getColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR);
        applyColorFilter(selectAllBtn, color);
        applyColorFilter(cutBtn, color);
        applyColorFilter(copyBtn, color);
        applyColorFilter(pasteBtn, color);
        applyColorFilter(longSelectBtn, color);
        // 应用颜色到所有额外按钮
        for (ExtraButtonEntry entry : extraButtonEntries) {
            applyColorFilter(entry.button, color);
        }
    }

    protected void subscribeEvents() {
        eventManager.subscribeAlways(SelectionChangeEvent.class, this::onSelectionChange);
        eventManager.subscribeAlways(ScrollEvent.class, this::onEditorScroll);
        eventManager.subscribeAlways(HandleStateChangeEvent.class, this::onHandleStateChange);
        eventManager.subscribeAlways(LongPressEvent.class, this::onEditorLongPress);
        eventManager.subscribeAlways(EditorFocusChangeEvent.class, this::onEditorFocusChange);
        eventManager.subscribeAlways(EditorReleaseEvent.class, this::onEditorRelease);
        eventManager.subscribeAlways(ColorSchemeUpdateEvent.class, this::onEditorColorChange);
        eventManager.subscribeAlways(DragSelectStopEvent.class, this::onDragSelectingStop);
    }

    protected void onEditorColorChange(@NonNull ColorSchemeUpdateEvent event) {
        applyColorScheme();
    }

    protected void onEditorFocusChange(@NonNull EditorFocusChangeEvent event) {
        if (!event.isGainFocus()) {
            dismiss();
        }
    }

    protected void onDragSelectingStop(@NonNull DragSelectStopEvent event) {
        displayWindow();
    }

    protected void onEditorRelease(@NonNull EditorReleaseEvent event) {
        setEnabled(false);
    }

    protected void onEditorLongPress(@NonNull LongPressEvent event) {
        if (editor.getCursor().isSelected() && lastCause == SelectionChangeEvent.CAUSE_SEARCH) {
            var idx = event.getIndex();
            if (idx >= editor.getCursor().getLeft() && idx <= editor.getCursor().getRight()) {
                lastCause = 0;
                displayWindow();
            }
            event.intercept(InterceptTarget.TARGET_EDITOR);
        }
    }

    protected void onEditorScroll(@NonNull ScrollEvent event) {
        var last = lastScroll;
        lastScroll = System.currentTimeMillis();
        if (lastScroll - last < DELAY && lastCause != SelectionChangeEvent.CAUSE_SEARCH) {
            postDisplay();
        }
    }

    protected void onHandleStateChange(@NonNull HandleStateChangeEvent event) {
        if (event.isHeld()) {
            postDisplay();
        }
        if (!event.getEditor().getCursor().isSelected()
                && event.getHandleType() == HandleStateChangeEvent.HANDLE_TYPE_INSERT
                && !event.isHeld()) {
            displayWindow();
            // Also, post to hide the window on handle disappearance
            editor.postDelayedInLifecycle(new Runnable() {
                @Override
                public void run() {
                    if (!editor.getEventHandler().shouldDrawInsertHandle()
                            && !editor.getCursor().isSelected()) {
                        dismiss();
                    } else if (!editor.getCursor().isSelected()) {
                        editor.postDelayedInLifecycle(this, CHECK_FOR_DISMISS_INTERVAL);
                    }
                }
            }, CHECK_FOR_DISMISS_INTERVAL);
        }
    }

    protected void onSelectionChange(@NonNull SelectionChangeEvent event) {
        if (handler.hasAnyHeldHandle() || event.getCause() == SelectionChangeEvent.CAUSE_DEAD_KEYS) {
            return;
        }
        if (handler.isDragSelecting()) {
            dismiss();
            return;
        }
        lastCause = event.getCause();
        if (event.isSelected() || event.getCause() == SelectionChangeEvent.CAUSE_LONG_PRESS && editor.getText().length() == 0) {
            // Always post show. See #193
            if (event.getCause() != SelectionChangeEvent.CAUSE_SEARCH) {
                editor.postInLifecycle(this::displayWindow);
            } else {
                dismiss();
            }
            lastPosition = -1;
        } else {
            var show = false;
            if (event.getCause() == SelectionChangeEvent.CAUSE_TAP && event.getLeft().index == lastPosition && !isShowing() && !editor.getText().isInBatchEdit() && editor.isEditable()) {
                editor.postInLifecycle(this::displayWindow);
                show = true;
            } else {
                dismiss();
            }
            if (event.getCause() == SelectionChangeEvent.CAUSE_TAP && !show) {
                lastPosition = event.getLeft().index;
            } else {
                lastPosition = -1;
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        eventManager.setEnabled(enabled);
        if (!enabled) {
            dismiss();
        }
    }

    /**
     * Get the view root of the panel.
     * <p>
     * Root view is {@link android.widget.LinearLayout}
     * Inside is a {@link android.widget.HorizontalScrollView}
     *
     * @see R.id#panel_root
     * @see R.id#panel_hv
     * @see R.id#panel_btn_select_all
     * @see R.id#panel_btn_copy
     * @see R.id#panel_btn_cut
     * @see R.id#panel_btn_paste
     */
    public ViewGroup getView() {
        return (ViewGroup) getPopup().getContentView();
    }

    private void postDisplay() {
        if (!isShowing()) {
            return;
        }
        dismiss();
        if (!editor.getCursor().isSelected()) {
            return;
        }
        editor.postDelayedInLifecycle(new Runnable() {
            @Override
            public void run() {
                if (!handler.hasAnyHeldHandle() && !editor.getSnippetController().isInSnippet() && System.currentTimeMillis() - lastScroll > DELAY
                        && editor.getScroller().isFinished()) {
                    displayWindow();
                } else {
                    editor.postDelayedInLifecycle(this, DELAY);
                }
            }
        }, DELAY);
    }

    private int selectTop(@NonNull RectF rect) {
        var rowHeight = editor.getRowHeight();
        if (rect.top - rowHeight * 3 / 2F > getHeight()) {
            return (int) (rect.top - rowHeight * 3 / 2 - getHeight());
        } else {
            return (int) (rect.bottom + rowHeight / 2);
        }
    }

    public void displayWindow() {
        updateBtnState();
        int top;
        var cursor = editor.getCursor();
        if (cursor.isSelected()) {
            var leftRect = editor.getLeftHandleDescriptor().position;
            var rightRect = editor.getRightHandleDescriptor().position;
            var top1 = selectTop(leftRect);
            var top2 = selectTop(rightRect);
            top = Math.min(top1, top2);
        } else {
            top = selectTop(editor.getInsertHandleDescriptor().position);
        }
        top = Math.max(0, Math.min(top, editor.getHeight() - getHeight() - 5));
        float handleLeftX = editor.getOffset(editor.getCursor().getLeftLine(), editor.getCursor().getLeftColumn());
        float handleRightX = editor.getOffset(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
        int panelX = (int) ((handleLeftX + handleRightX) / 2f - rootView.getMeasuredWidth() / 2f);
        setLocationAbsolutely(panelX, top);
        show();
    }

    /**
     * Update the state of paste button
     */
    private void updateBtnState() {
        pasteBtn.setEnabled(editor.hasClip());
        copyBtn.setVisibility(editor.getCursor().isSelected() ? View.VISIBLE : View.GONE);
        pasteBtn.setVisibility(editor.isEditable() ? View.VISIBLE : View.GONE);
        cutBtn.setVisibility((editor.getCursor().isSelected() && editor.isEditable()) ? View.VISIBLE : View.GONE);
        longSelectBtn.setVisibility((!editor.getCursor().isSelected() && editor.isEditable()) ? View.VISIBLE : View.GONE);
        
        // 更新额外按钮的可见性
        updateExtraButtonVisibility();
        
        rootView.measure(View.MeasureSpec.makeMeasureSpec(1000000, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(100000, View.MeasureSpec.AT_MOST));
        setSize(Math.min(rootView.getMeasuredWidth(), (int) (editor.getDpUnit() * 280)), getHeight());
    }
    
    /**
     * 更新所有额外按钮的可见性
     */
    private void updateExtraButtonVisibility() {
        for (ExtraButtonEntry entry : extraButtonEntries) {
            boolean shouldShow = entry.provider.shouldShowButton(editor);
            entry.button.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * 添加额外按钮提供者
     * @param provider 按钮提供者
     */
    public void addExtraButtonProvider(@NonNull ExtraButtonProvider provider) {
        if (buttonContainer == null) {
            return;
        }
        
        // 检查是否已经添加过相同的提供者
        for (ExtraButtonEntry entry : extraButtonEntries) {
            if (entry.provider == provider) {
                return;
            }
        }
        
        // 创建新的额外按钮
        ImageButton button = new ImageButton(editor.getContext());
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
            (int) (45 * editor.getDpUnit()),
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT
        );
        button.setLayoutParams(params);
        button.setImageResource(provider.getIconResource());
        button.setBackgroundResource(android.R.color.transparent);
        button.setContentDescription(provider.getContentDescription());
        button.setVisibility(View.GONE);
        button.setOnClickListener(v -> {
            provider.onButtonClick(editor);
            dismiss();
        });
        
        // 应用颜色
        int color = editor.getColorScheme().getColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR);
        applyColorFilter(button, color);
        
        // 添加到容器
        buttonContainer.addView(button);
        
        // 保存到列表
        extraButtonEntries.add(new ExtraButtonEntry(provider, button));
    }
    
    /**
     * 移除额外按钮提供者
     * @param provider 要移除的按钮提供者
     */
    public void removeExtraButtonProvider(@NonNull ExtraButtonProvider provider) {
        if (buttonContainer == null) {
            return;
        }
        
        ExtraButtonEntry toRemove = null;
        for (ExtraButtonEntry entry : extraButtonEntries) {
            if (entry.provider == provider) {
                toRemove = entry;
                break;
            }
        }
        
        if (toRemove != null) {
            buttonContainer.removeView(toRemove.button);
            extraButtonEntries.remove(toRemove);
        }
    }
    
    /**
     * 清除所有额外按钮提供者
     */
    public void clearExtraButtonProviders() {
        if (buttonContainer == null) {
            return;
        }
        
        for (ExtraButtonEntry entry : extraButtonEntries) {
            buttonContainer.removeView(entry.button);
        }
        extraButtonEntries.clear();
    }
    
    /**
     * 获取所有额外按钮提供者
     */
    @NonNull
    public List<ExtraButtonProvider> getExtraButtonProviders() {
        List<ExtraButtonProvider> providers = new ArrayList<>();
        for (ExtraButtonEntry entry : extraButtonEntries) {
            providers.add(entry.provider);
        }
        return providers;
    }
    
    /**
     * 额外按钮条目，保存提供者和对应的按钮
     */
    private static class ExtraButtonEntry {
        final ExtraButtonProvider provider;
        final ImageButton button;
        
        ExtraButtonEntry(ExtraButtonProvider provider, ImageButton button) {
            this.provider = provider;
            this.button = button;
        }
    }
    
    /**
     * 额外按钮提供者接口
     */
    public interface ExtraButtonProvider {
        /**
         * 获取按钮图标资源 ID
         */
        int getIconResource();
        
        /**
         * 获取按钮的内容描述（用于无障碍）
         */
        @Nullable
        String getContentDescription();
        
        /**
         * 判断是否应该显示按钮
         * @param editor 编辑器实例
         * @return 如果应该显示返回 true
         */
        boolean shouldShowButton(@NonNull CodeEditor editor);
        
        /**
         * 按钮点击回调
         * @param editor 编辑器实例
         */
        void onButtonClick(@NonNull CodeEditor editor);
    }

    @Override
    public void show() {
        if (!enabled || editor.getSnippetController().isInSnippet() || !editor.hasFocus() || editor.isInMouseMode()) {
            return;
        }
        super.show();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.panel_btn_select_all) {
            editor.selectAll();
            return;
        } else if (id == R.id.panel_btn_cut) {
            if (editor.getCursor().isSelected()) {
                editor.cutText();
            }
        } else if (id == R.id.panel_btn_paste) {
            editor.pasteText();
            editor.setSelection(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
        } else if (id == R.id.panel_btn_copy) {
            editor.copyText();
            editor.setSelection(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
        } else if (id == R.id.panel_btn_long_select) {
            editor.beginLongSelect();
        }
        dismiss();
    }

}

