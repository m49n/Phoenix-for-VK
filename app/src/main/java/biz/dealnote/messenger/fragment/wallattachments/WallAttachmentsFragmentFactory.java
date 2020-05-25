package biz.dealnote.messenger.fragment.wallattachments;

import androidx.fragment.app.Fragment;

import biz.dealnote.messenger.api.model.VKApiAttachment;

public class WallAttachmentsFragmentFactory {

    public static Fragment newInstance(int accountId, int ownerId, String type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cant bee null");
        }

        Fragment fragment = null;
        switch (type) {
            case VKApiAttachment.TYPE_PHOTO:
                fragment = WallPhotosAttachmentsFragment.newInstance(accountId, ownerId);
                break;
            case VKApiAttachment.TYPE_VIDEO:
                fragment = WallVideosAttachmentsFragment.newInstance(accountId, ownerId);
                break;
            case VKApiAttachment.TYPE_DOC:
                fragment = WallDocsAttachmentsFragment.newInstance(accountId, ownerId);
                break;
            case VKApiAttachment.TYPE_LINK:
                fragment = WallLinksAttachmentsFragment.newInstance(accountId, ownerId);
                break;
            case VKApiAttachment.TYPE_AUDIO:
                fragment = WallAudiosAttachmentsFragment.newInstance(accountId, ownerId);
                break;
        }

        return fragment;
    }
}
