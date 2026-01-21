/*
 * {EasyGank}  Copyright (C) {2015}  {CaMnter}
 *
 * This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
 * This is free software, and you are welcome to redistribute it
 * under certain conditions; type `show c' for details.
 *
 * The hypothetical commands `show w' and `show c' should show the appropriate
 * parts of the General Public License.  Of course, your program's commands
 * might be different; for a GUI interface, you would use an "about box".
 *
 * You should also get your employer (if you work as a programmer) or school,
 * if any, to sign a "copyright disclaimer" for the program, if necessary.
 * For more information on this, and how to apply and follow the GNU GPL, see
 * <http://www.gnu.org/licenses/>.
 *
 * The GNU General Public License does not permit incorporating your program
 * into proprietary programs.  If your program is a subroutine library, you
 * may consider it more useful to permit linking proprietary applications with
 * the library.  If this is what you want to do, use the GNU Lesser General
 * Public License instead of this License.  But first, please read
 * <http://www.gnu.org/philosophy/why-not-lgpl.html>.
 */

package com.google.easyapp.utils;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.google.easyapp.APPUtils;


/**
 * Description：GlideUtils
 * Created by：CaMnter
 * Time：2016-01-04 22:19
 */
public class GlideUtils {

    private static final String TAG = "GlideUtils";


    public static void displayAssetsImage(ImageView view, String url) {
        displayImage(view, "file:///android_asset/" + url);
    }

    public static void displayAssetsImage(ImageView view, String url, RequestListener<? super String, GlideDrawable> requestListener) {
        displayImage(view, "file:///android_asset/" + url, requestListener);
    }

    public static void displayLocalFileImage(ImageView view, String url) {
        displayImage(view, "file:///" + url);
    }

    public static void displayLocalFileImage(ImageView view, String url, RequestListener<? super String, GlideDrawable> requestListener) {
        displayImage(view, "file:///" + url, requestListener);
    }

    public static void displayImage(final ImageView view, String url) {
        if (view != null) {
            Context context = view.getContext();
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (!activity.isFinishing()) {
                    Glide.with(APPUtils.getApp())
                            .load(url)
                            .into(view);
                }
            }

        }
    }
//    public static void displayLocalFileImage(ImageView view, String url) {
//        if (view != null) {
//            Context context = view.getContext();
//            if (context instanceof Activity) {
//                Activity activity = (Activity) context;
//                if (!activity.isFinishing()) {
//                    Glide.with(EasyLibUtils.getApp())
//                            .fromFile()
//                            .load(new File(url))
//                            .into(view);
//                }
//            }
//        }
//    }
    public static void displayImage(final ImageView view, String url, RequestListener<? super String, GlideDrawable> requestListener) {
        if (view != null) {
            Context context = view.getContext();
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (!activity.isFinishing()) {
                    Glide.with(APPUtils.getApp())
                            .load(url)
                            .listener(requestListener)
                            .into(view);
                }
            }

        }
    }
}
