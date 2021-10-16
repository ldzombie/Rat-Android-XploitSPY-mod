package com.remote.app.Managers;

public class base {
    public static class CALLS {
        public static final String CALL_TYPE = "type";
        public static final String CALL_PHONE = "phoneNumber";
        public static final String CALL_NAME = "name";
        public static final String CALL_DURATION = "duration";
        public static final String CALL_DATE = "date";
    }

    public static class CONTACTS {
        public static final String CONTACT_ID = "raw_id";
        public static final String CONTACT_NAME = "name";
        public static final String CONTACT_PHONE = "phoneNumber";
    }

    public static class MediaFiles {
        public static final String ID = "id";
        public static final String DISPLAY_NAME = "name";
        public static final String PATH = "path";
        public static final String PHOTOS = "photos";
        public static final String VIDEOS = "videos";
        public static final String BUCKET= "bucket";
        public static final String DATE = "date";
        public static final String DATA = "imgData";
    }

    public static class FILES {
        public static final String FILE_NAME= "name";
        public static final String FILE_isDir= "isDir";
        public static final String FILE_PATH= "path";
        public static final String FILE_TYPE= "type";
        public static final String FILE_BUFFER= "buffer";
    }

    public static class SMS {
        public static final String SMS_BODY= "body";
        public static final String SMS_READ= "read";
        public static final String SMS_TYPE= "type";
        public static final String SMS_ADDRESS= "address";
        public static final String SMS_ID= "_id";
        public static final String SMS_DATE= "date";
        public static final String SMS_LIST= "smslist";
    }

    public static class DOWNLOADS {
        public static final String DOWN_ID = "downId";
        public static final String DOWN_URL = "downUrl";
        public static final String DOWN_TYPE = "downType";
        public static final String DOWN_PATH = "downPath";
        public static final String DOWN_TITLE = "downTitle";
    }

    public static class AudioStreams{
        public final static int STREAM_VOICE_CALL =0;
        public final static int STREAM_SYSTEM =1;
        public final static int STREAM_RING =2;
        public final static int STREAM_MUSIC =3;
        public final static int STREAM_ALARM =4;
        public final static int STREAM_NOTIFICATION =5;
        public final static int STREAM_ACCESSIBILITY =10;
    }

    public static class APPS{
        public final static String APPS ="apps";
        public final static String APP_NAME ="appName";
        public final static String PACKAGE_NAME ="packageName";
        public final static String VERSION_NAME ="versionName";
        public final static String VERSION_CODE ="versionCode";
    }

    public static class NOTOFICATIONS{
        public final static String APP_NAME ="appName";
        public final static String TITLE ="title";
        public final static String CONTENT ="content";
        public final static String POST_TIME ="postTime";
        public final static String KEY ="key";
    }

    public static class LOGGER{
        public final static String TIME ="time";
        public final static String TYPE ="type";
        public final static String PACKAGE ="package";
        public final static String TEXT ="text";
    }

    public static class PERMISSION{
        public final static String PERMISSIONS ="permissions";
    }

    public static class CLIPBOARD{
        public final static String TEXT ="text";
        public final static String DATA ="date";
    }

    public static class WIFI{
        public final static String BSSID ="BSSID";
        public final static String SSID ="SSID";
        public final static String NETWORKS ="networks";
        public final static String PWD ="PWD";
        public final static String ROOT ="root";
    }
}
