package kr.pagero.calltag;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/** Android telephony 프로세스가 자동발송용 MMS PDU를 읽도록 제공하는 읽기 전용 provider. */
public final class MmsPduProvider extends ContentProvider {
    public static final String PATH = "outbox";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.wap.mms-message";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (getContext() == null) throw new FileNotFoundException("Context unavailable");
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        if (uri == null || uri.getPathSegments().size() != 2
                || !PATH.equals(uri.getPathSegments().get(0))) {
            throw new FileNotFoundException("Invalid MMS URI");
        }
        String fileName = uri.getLastPathSegment();
        if (fileName == null || fileName.contains("/") || fileName.contains("..")) {
            throw new FileNotFoundException("Invalid MMS file");
        }
        File directory = new File(getContext().getCacheDir(), "mms_outbox");
        File file = new File(directory, fileName);
        try {
            String root = directory.getCanonicalPath() + File.separator;
            if (!file.getCanonicalPath().startsWith(root)) {
                throw new FileNotFoundException("Invalid MMS path");
            }
        } catch (IOException error) {
            throw new FileNotFoundException("Invalid MMS path");
        }
        if (!file.isFile()) throw new FileNotFoundException("MMS file missing");
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read only");
    }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only");
    }
    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) {
        throw new UnsupportedOperationException("Read only");
    }
}
