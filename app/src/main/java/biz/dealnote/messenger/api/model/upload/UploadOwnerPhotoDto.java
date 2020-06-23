package biz.dealnote.messenger.api.model.upload;

import com.google.gson.annotations.SerializedName;

public class UploadOwnerPhotoDto {

    @SerializedName("server")
    public String server;

    @SerializedName("photo")
    public String photo;

    @SerializedName("hash")
    public String hash;

    @Override
    public String toString() {
        return "UploadOwnerPhotoDto{" +
                "server=" + server +
                ", photo='" + photo + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}
