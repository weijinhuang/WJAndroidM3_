package com.wj.basecomponent.view.viewpager;

import android.graphics.PointF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class PagerSnapHelperImpl extends PagerSnapHelper {
     FakeDrag mFakeDragger;
    int mSpanSize;

    PagerSnapHelperImpl( FakeDrag fakeDragger, int spanCount) {
        mFakeDragger = fakeDragger;
        mSpanSize = spanCount;
    }

    public int getSpanCount() {
        return mSpanSize;
    }

    public void setSpanCount(int spanCount) {
        this.mSpanSize = spanCount;
    }

    private boolean isReverseLayout(RecyclerView.LayoutManager layoutManager) {
        final int itemCount = layoutManager.getItemCount();
        if ((layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
            RecyclerView.SmoothScroller.ScrollVectorProvider vectorProvider =
                    (RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager;
            PointF vectorForEnd = vectorProvider.computeScrollVectorForPosition(itemCount - 1);
            if (vectorForEnd != null) {
                return vectorForEnd.x < 0 || vectorForEnd.y < 0;
            }
        }
        return false;
    }

    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX,
                                      int velocityY) {
        final int itemCount = layoutManager.getItemCount();
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION;
        }

        final OrientationHelper orientationHelper = OrientationHelper.createHorizontalHelper(layoutManager);
        if (orientationHelper == null) {
            return RecyclerView.NO_POSITION;
        }

        // A child that is exactly in the center is eligible for both before and after
        View closestChildBeforeCenter = null;
        int distanceBefore = Integer.MIN_VALUE;
        View closestChildAfterCenter = null;
        int distanceAfter = Integer.MAX_VALUE;

        // Find the first view before the center, and the first view after the center
        final int childCount = layoutManager.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = layoutManager.getChildAt(i);
            if (child == null) {
                continue;
            }
            final int distance = distanceToCenter(layoutManager, child, orientationHelper);

            if (distance <= 0 && distance > distanceBefore) {
                // Child is before the center and closer then the previous best
                distanceBefore = distance;
                closestChildBeforeCenter = child;
            }
            if (distance >= 0 && distance < distanceAfter) {
                // Child is after the center and closer then the previous best
                distanceAfter = distance;
                closestChildAfterCenter = child;
            }
        }

        // Return the position of the first child from the center, in the direction of the fling
        final boolean forwardDirection = isForwardFling(layoutManager, velocityX, velocityY);
//        WJLog.Companion.d("forwardDirection:" + forwardDirection);
        if (forwardDirection && closestChildAfterCenter != null) {
//            WJLog.Companion.d("closestChildAfterCenter:" + ((TextView) closestChildAfterCenter).getText());
            int position = layoutManager.getPosition(closestChildAfterCenter);
            if (mSpanSize != 1) {
                if (position % 4 != 0) {
                    if (forwardDirection) {
                        position += 2;
                    } else {
                        position -= 2;
                    }
                }
            }
            return position;
        } else if (!forwardDirection && closestChildBeforeCenter != null) {
//            WJLog.Companion.d("closestChildBeforeCenter:" + ((TextView) closestChildBeforeCenter).getText());
            int position = layoutManager.getPosition(closestChildBeforeCenter);
            if (mSpanSize != 1) {
                if (position % 4 != 0) {
                    if (forwardDirection) {
                        position += mSpanSize;
                    } else {
                        position -= mSpanSize;
                    }
                }
            }
            return position;
        }

        // There is no child in the direction of the fling. Either it doesn't exist (start/end of
        // the list), or it is not yet attached (very rare case when children are larger then the
        // viewport). Extrapolate from the child that is visible to get the position of the view to
        // snap to.
        View visibleView = forwardDirection ? closestChildBeforeCenter : closestChildAfterCenter;
        if (visibleView == null) {
            return RecyclerView.NO_POSITION;
        }
        int visiblePosition = layoutManager.getPosition(visibleView);
        int snapToPosition = visiblePosition
                + (isReverseLayout(layoutManager) == forwardDirection ? -1 : +1);

        if (snapToPosition < 0 || snapToPosition >= itemCount) {
            return RecyclerView.NO_POSITION;
        }
//        WJLog.Companion.d("snapToPosition:" + snapToPosition);
        return snapToPosition;
    }

    private boolean isForwardFling(RecyclerView.LayoutManager layoutManager, int velocityX,
                                   int velocityY) {
        if (layoutManager.canScrollHorizontally()) {
            return velocityX > 0;
        } else {
            return velocityY > 0;
        }
    }

    private View findCenterView(RecyclerView.LayoutManager layoutManager,
                                OrientationHelper helper) {
        int childCount = layoutManager.getChildCount();
        if (childCount == 0) {
            return null;
        }

        View closestChild = null;
        View preClosestChild = null;
        View nextClosestChild = null;
        final int center = helper.getStartAfterPadding() + helper.getTotalSpace() / 2;
        int absClosest = Integer.MAX_VALUE;

        for (int i = 0; i < childCount; i++) {
            final View child = layoutManager.getChildAt(i);

            int childCenter = helper.getDecoratedStart(child)
                    + (helper.getDecoratedMeasurement(child) / 2);
            int absDistance = Math.abs(childCenter - center);
            /* if child center is closer than previous closest, set it as closest  */
            nextClosestChild = child;
            if (absDistance < absClosest) {
                absClosest = absDistance;
                preClosestChild = closestChild;
                closestChild = child;

            }
        }

        if (null != closestChild) {
            int viewPosition = layoutManager.getPosition(closestChild);
//            WJLog.Companion.d("findCenter:viewPosition:" + viewPosition);
            if (mSpanSize == 1) {
                return closestChild;
            }
            int i = 0;
            if (mSpanSize != 1) {
                i = viewPosition % 4;
            }
            if (i == 0) {
                return closestChild;
            } else if (i < 2) {
                if (null != preClosestChild) {
                    return preClosestChild;
                } else {
                    return nextClosestChild;
                }
            } else {
                if (null != nextClosestChild) {
                    return nextClosestChild;
                } else {
                    return preClosestChild;
                }
            }
        }
        return closestChild;
    }


    @Nullable
    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        // When interrupting a smooth scroll with a fake drag, we stop RecyclerView's scroll
        // animation, which fires a scroll state change to IDLE. PagerSnapHelper then kicks in
        // to snap to a page, which we need to prevent here.
        // Simplifying that case: during a fake drag, no snapping should occur.
        if (mFakeDragger.isFakeDragging()) {
            return null;
        } else {
            if (layoutManager.canScrollVertically()) {
                return findCenterView(layoutManager, OrientationHelper.createVerticalHelper(layoutManager));
            } else if (layoutManager.canScrollHorizontally()) {
                return findCenterView(layoutManager, OrientationHelper.createHorizontalHelper(layoutManager));
            } else {
                return null;
            }
        }

    }

    @Override
    public int[] calculateScrollDistance(int velocityX, int velocityY) {
        int[] ints = super.calculateScrollDistance(velocityX, velocityY);
        return ints;
    }

    @Nullable
    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        int[] out = new int[2];
        ;
        if (layoutManager.canScrollHorizontally()) {
            out[0] = distanceToCenter(layoutManager, targetView,
                    OrientationHelper.createHorizontalHelper(layoutManager));
        } else {
            out[0] = 0;
        }

        if (layoutManager.canScrollVertically()) {
            out[1] = distanceToCenter(layoutManager, targetView,
                    OrientationHelper.createHorizontalHelper(layoutManager));
        } else {
            out[1] = 0;
        }
//        WJLog.Companion.d("calculateDistanceToFinalSnap:" + out[0]);
        return out;
    }

    private int distanceToCenter(@NonNull RecyclerView.LayoutManager layoutManager,
                                 @NonNull View targetView, OrientationHelper helper) {
        final int childCenter = helper.getDecoratedStart(targetView)
                + (helper.getDecoratedMeasurement(targetView) / 2);

        final int containerCenter;
        if (mSpanSize != 1) {
            containerCenter = (helper.getStartAfterPadding() + helper.getTotalSpace()) / 4;
        } else {
            containerCenter = (helper.getStartAfterPadding() + helper.getTotalSpace()) / 2;
        }
        return childCenter - containerCenter;
    }
}
