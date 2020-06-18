package biz.dealnote.messenger.media.gif;

import androidx.annotation.NonNull;

import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.settings.IProxySettings;
import biz.dealnote.messenger.settings.ISettings;

public class AppGifPlayerFactory implements IGifPlayerFactory {

    private final IProxySettings proxySettings;

    public AppGifPlayerFactory(IProxySettings proxySettings, ISettings.IOtherSettings otherSettings) {
        this.proxySettings = proxySettings;
    }

    @Override
    public IGifPlayer createGifPlayer(@NonNull String url) {
        ProxyConfig config = proxySettings.getActiveProxy();
        return new ExoGifPlayer(url, config);
    }
}