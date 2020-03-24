package biz.dealnote.messenger.model.selection;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by admin on 08.10.2016.
 * phoenix
 */
@IntDef({Types.LOCAL_PHOTOS, Types.VK_PHOTOS, Types.FILES, Types.VIDEOS})
@Retention(RetentionPolicy.SOURCE)
public @interface Types {
    int LOCAL_PHOTOS = 0;
    int VK_PHOTOS = 1;
    int FILES = 2;
    int VIDEOS = 3;
}