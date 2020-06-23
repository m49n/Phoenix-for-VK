package biz.dealnote.messenger.media.voice;

import android.content.Context;

import androidx.annotation.NonNull;

import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.settings.IProxySettings;
import biz.dealnote.messenger.settings.ISettings;

public class VoicePlayerFactory implements IVoicePlayerFactory {

    private final Context app;
    private final IProxySettings proxySettings;

    public VoicePlayerFactory(Context context, IProxySettings proxySettings, ISettings.IOtherSettings otherSettings) {
        this.app = context.getApplicationContext();
        this.proxySettings = proxySettings;
    }

    @NonNull
    @Override
    public IVoicePlayer createPlayer() {
        ProxyConfig config = proxySettings.getActiveProxy();
        return new ExoVoicePlayer(app, config);
    }
}