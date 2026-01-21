// Generated code from Butter Knife. Do not modify!
package mcv.testfacepass;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.DebouncingOnClickListener;
import butterknife.internal.Utils;
import java.lang.IllegalStateException;
import java.lang.Override;

public class FaceManageActivity_ViewBinding implements Unbinder {
  private FaceManageActivity target;

  private View view2131230881;

  private View view2131230845;

  private View view2131230846;

  @UiThread
  public FaceManageActivity_ViewBinding(FaceManageActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public FaceManageActivity_ViewBinding(final FaceManageActivity target, View source) {
    this.target = target;

    View view;
    view = Utils.findRequiredView(source, R.id.ok, "method 'onClick'");
    view2131230881 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onClick(p0);
      }
    });
    view = Utils.findRequiredView(source, R.id.iv_add, "method 'onClick'");
    view2131230845 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onClick(p0);
      }
    });
    view = Utils.findRequiredView(source, R.id.iv_add_image, "method 'onClick'");
    view2131230846 = view;
    view.setOnClickListener(new DebouncingOnClickListener() {
      @Override
      public void doClick(View p0) {
        target.onClick(p0);
      }
    });
  }

  @Override
  @CallSuper
  public void unbind() {
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    target = null;


    view2131230881.setOnClickListener(null);
    view2131230881 = null;
    view2131230845.setOnClickListener(null);
    view2131230845 = null;
    view2131230846.setOnClickListener(null);
    view2131230846 = null;
  }
}
