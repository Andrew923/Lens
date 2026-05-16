package com.andrew.lens;

import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Two-page adapter for the Lens pager. Deliberately Fragment-free: ViewPager2
 * only needs a {@link RecyclerView.Adapter}; FragmentStateAdapter (the only
 * path requiring Fragments) is not used, which suits a VoiceInteractionSession
 * window that has no FragmentActivity host.
 *
 * Page 0 = {@link TranslateScreenView}, page 1 = {@link CopyScreenView}. The
 * two screen views are created once by {@link LensPagerView} and reused; each
 * is hosted in its own stable ViewHolder (item view type == position), so with
 * offscreenPageLimit=1 both stay alive and retain their screenshot/state.
 */
public class LensPagerAdapter extends RecyclerView.Adapter<LensPagerAdapter.ScreenHolder> {

    static class ScreenHolder extends RecyclerView.ViewHolder {
        final FrameLayout container;
        ScreenHolder(FrameLayout container) {
            super(container);
            this.container = container;
        }
    }

    private final TranslateScreenView translateScreen;
    private final CopyScreenView copyScreen;

    public LensPagerAdapter(TranslateScreenView translateScreen, CopyScreenView copyScreen) {
        this.translateScreen = translateScreen;
        this.copyScreen = copyScreen;
    }

    @Override
    public int getItemViewType(int position) {
        return position; // stable: page 0 and page 1 never share a holder
    }

    @NonNull
    @Override
    public ScreenHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout container = new FrameLayout(parent.getContext());
        container.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new ScreenHolder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull ScreenHolder holder, int position) {
        FrameLayout screen = position == 0 ? translateScreen : copyScreen;
        if (screen.getParent() instanceof ViewGroup) {
            ((ViewGroup) screen.getParent()).removeView(screen);
        }
        holder.container.removeAllViews();
        holder.container.addView(screen, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
