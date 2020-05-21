package biz.dealnote.messenger.link;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsIntent;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.fragment.VKPhotosFragment;
import biz.dealnote.messenger.fragment.fave.FaveTabsFragment;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.NewsFeedCriteria;
import biz.dealnote.messenger.link.types.AbsLink;
import biz.dealnote.messenger.link.types.AudioPlaylistLink;
import biz.dealnote.messenger.link.types.AudiosLink;
import biz.dealnote.messenger.link.types.BoardLink;
import biz.dealnote.messenger.link.types.DialogLink;
import biz.dealnote.messenger.link.types.DocLink;
import biz.dealnote.messenger.link.types.DomainLink;
import biz.dealnote.messenger.link.types.FaveLink;
import biz.dealnote.messenger.link.types.FeedSearchLink;
import biz.dealnote.messenger.link.types.OwnerLink;
import biz.dealnote.messenger.link.types.PageLink;
import biz.dealnote.messenger.link.types.PhotoAlbumLink;
import biz.dealnote.messenger.link.types.PhotoAlbumsLink;
import biz.dealnote.messenger.link.types.PhotoLink;
import biz.dealnote.messenger.link.types.PollLink;
import biz.dealnote.messenger.link.types.TopicLink;
import biz.dealnote.messenger.link.types.VideoLink;
import biz.dealnote.messenger.link.types.WallCommentLink;
import biz.dealnote.messenger.link.types.WallLink;
import biz.dealnote.messenger.link.types.WallPostLink;
import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.CommentedType;
import biz.dealnote.messenger.model.Peer;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.PhoenixToast;

import static androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.singletonArrayList;

public class LinkHelper {


    public static void openUrl(Activity context, int accountId, String link) {
        if (link == null || link.length() <= 0) {
            PhoenixToast.CreatePhoenixToast(context).showToastError(R.string.empty_clipboard_url);
            return;
        }
        if (!openVKlink(context, accountId, link)) {
            PlaceFactory.getExternalLinkPlace(accountId, link).tryOpenWith(context);
        }
    }

    public static boolean openVKLink(Activity activity, int accountId, AbsLink link) {
        switch (link.type) {

            case AbsLink.PLAYLIST:
                AudioPlaylistLink plLink = (AudioPlaylistLink) link;
                PlaceFactory.getAudiosInAlbumPlace(accountId, plLink.ownerId, plLink.playlistId, plLink.access_key).tryOpenWith(activity);
                break;

            case AbsLink.POLL:
                PollLink pollLink = (PollLink) link;
                openLinkInBrowser(activity, "https://vk.com/poll" + pollLink.ownerId + "_" + pollLink.Id);
                break;

            case AbsLink.WALL_COMMENT:
                WallCommentLink wallCommentLink = (WallCommentLink) link;

                Commented commented = new Commented(wallCommentLink.getPostId(), wallCommentLink.getOwnerId(), CommentedType.POST, null);
                PlaceFactory.getCommentsPlace(accountId, commented, wallCommentLink.getCommentId()).tryOpenWith(activity);
                break;

            case AbsLink.DIALOGS:
                PlaceFactory.getDialogsPlace(accountId, accountId, null, 0).tryOpenWith(activity);
                break;

            case AbsLink.PHOTO:
                PhotoLink photoLink = (PhotoLink) link;

                Photo photo = new Photo()
                        .setId(photoLink.id)
                        .setOwnerId(photoLink.ownerId);

                PlaceFactory.getSimpleGalleryPlace(accountId, singletonArrayList(photo), 0, true).tryOpenWith(activity);
                break;

            case AbsLink.PHOTO_ALBUM:
                PhotoAlbumLink photoAlbumLink = (PhotoAlbumLink) link;
                PlaceFactory.getVKPhotosAlbumPlace(accountId, photoAlbumLink.ownerId,
                        photoAlbumLink.albumId, null).tryOpenWith(activity);
                break;

            case AbsLink.PROFILE:
            case AbsLink.GROUP:
                OwnerLink ownerLink = (OwnerLink) link;
                PlaceFactory.getOwnerWallPlace(accountId, ownerLink.ownerId, null).tryOpenWith(activity);
                break;

            case AbsLink.TOPIC:
                TopicLink topicLink = (TopicLink) link;
                PlaceFactory.getCommentsPlace(accountId, new Commented(topicLink.topicId, topicLink.ownerId,
                        CommentedType.TOPIC, null), null).tryOpenWith(activity);
                break;

            case AbsLink.WALL_POST:
                WallPostLink wallPostLink = (WallPostLink) link;
                PlaceFactory.getPostPreviewPlace(accountId, wallPostLink.postId, wallPostLink.ownerId)
                        .tryOpenWith(activity);
                break;

            case AbsLink.ALBUMS:
                PhotoAlbumsLink photoAlbumsLink = (PhotoAlbumsLink) link;
                PlaceFactory.getVKPhotoAlbumsPlace(accountId, photoAlbumsLink.ownerId, VKPhotosFragment.ACTION_SHOW_PHOTOS, null).tryOpenWith(activity);
                break;

            case AbsLink.DIALOG:
                DialogLink dialogLink = (DialogLink) link;
                Peer peer = new Peer(dialogLink.peerId);
                PlaceFactory.getChatPlace(accountId, accountId, peer, 0).tryOpenWith(activity);
                break;

            case AbsLink.WALL:
                WallLink wallLink = (WallLink) link;
                PlaceFactory.getOwnerWallPlace(accountId, wallLink.ownerId, null).tryOpenWith(activity);
                break;

            case AbsLink.VIDEO:
                VideoLink videoLink = (VideoLink) link;
                PlaceFactory.getVideoPreviewPlace(accountId, videoLink.ownerId, videoLink.videoId, null)
                        .tryOpenWith(activity);
                break;

            case AbsLink.AUDIOS:
                AudiosLink audiosLink = (AudiosLink) link;
                PlaceFactory.getAudiosPlace(accountId, audiosLink.ownerId).tryOpenWith(activity);
                break;

            case AbsLink.DOMAIN:
                DomainLink domainLink = (DomainLink) link;
                PlaceFactory.getResolveDomainPlace(accountId, domainLink.fullLink, domainLink.domain)
                        .tryOpenWith(activity);
                break;

            case AbsLink.PAGE:
                PlaceFactory.getWikiPagePlace(accountId, ((PageLink) link).getLink()).tryOpenWith(activity);
                break;

            case AbsLink.DOC:
                DocLink docLink = (DocLink) link;
                PlaceFactory.getDocPreviewPlace(accountId, docLink.docId, docLink.ownerId, null).tryOpenWith(activity);
                break;

            case AbsLink.FAVE:
                FaveLink faveLink = (FaveLink) link;
                int targetTab = FaveTabsFragment.getTabByLinkSection(faveLink.section);
                if (targetTab == FaveTabsFragment.TAB_UNKNOWN) {
                    return false;
                }

                PlaceFactory.getBookmarksPlace(accountId, targetTab).tryOpenWith(activity);
                break;

            case AbsLink.BOARD:
                BoardLink boardLink = (BoardLink) link;
                PlaceFactory.getTopicsPlace(accountId, -Math.abs(boardLink.getGroupId())).tryOpenWith(activity);
                break;

            case AbsLink.FEED_SEARCH:
                FeedSearchLink feedSearchLink = (FeedSearchLink) link;
                NewsFeedCriteria criteria = new NewsFeedCriteria(feedSearchLink.getQ());
                PlaceFactory.getSingleTabSearchPlace(accountId, SearchContentType.NEWS, criteria).tryOpenWith(activity);
                break;

            default:
                return false;
        }

        return true;
    }

    private static boolean openVKlink(Activity activity, int accoutnId, String url) {
        AbsLink link = VkLinkParser.parse(url);
        return link != null && openVKLink(activity, accoutnId, link);
    }

    public static ArrayList<ResolveInfo> getCustomTabsPackages(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));

        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
        ArrayList<ResolveInfo> packagesSupportingCustomTabs = new ArrayList<>();
        for (ResolveInfo info : resolvedActivityList) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            if (pm.resolveService(serviceIntent, 0) != null) {
                packagesSupportingCustomTabs.add(info);
            }
        }
        return packagesSupportingCustomTabs;
    }

    public static void openLinkInBrowser(Context context, String url) {
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
        intentBuilder.setToolbarColor(CurrentTheme.getColorPrimary(context));
        CustomTabsIntent customTabsIntent = intentBuilder.build();
        if (getCustomTabsPackages(context) != null && !getCustomTabsPackages(context).isEmpty()) {
            customTabsIntent.intent.setPackage(getCustomTabsPackages(context).get(0).resolvePackageName);
        }
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }

    public static void openLinkInBrowserInternal(Context context, int accoutnId, String url) {
        if (isEmpty(url))
            return;
        PlaceFactory.getExternalLinkPlace(accoutnId, url).tryOpenWith(context);
    }

    public static Commented findCommentedFrom(String url) {
        AbsLink link = VkLinkParser.parse(url);
        Commented commented = null;
        if (link != null) {
            switch (link.type) {
                case AbsLink.WALL_POST:
                    WallPostLink wallPostLink = (WallPostLink) link;
                    commented = new Commented(wallPostLink.postId, wallPostLink.ownerId, CommentedType.POST, null);
                    break;
                case AbsLink.PHOTO:
                    PhotoLink photoLink = (PhotoLink) link;
                    commented = new Commented(photoLink.id, photoLink.ownerId, CommentedType.PHOTO, null);
                    break;
                case AbsLink.VIDEO:
                    VideoLink videoLink = (VideoLink) link;
                    commented = new Commented(videoLink.videoId, videoLink.ownerId, CommentedType.VIDEO, null);
                    break;
                case AbsLink.TOPIC:
                    TopicLink topicLink = (TopicLink) link;
                    commented = new Commented(topicLink.topicId, topicLink.ownerId, CommentedType.TOPIC, null);
                    break;
            }
        }

        return commented;
    }
}
