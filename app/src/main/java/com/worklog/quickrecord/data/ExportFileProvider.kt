package com.worklog.quickrecord.data

import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.worklog.quickrecord.domain.ExportAction
import com.worklog.quickrecord.domain.ReportAccess
import com.worklog.quickrecord.reminder.ExportStamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 会记账的 FileProvider。
 *
 * 报告通过「内容地址」分享给别的 App 时，对方必须来读这个文件。用户只是在
 * 分享面板里看了一眼又退出来，是不会有 App 来读的。所以「文件被读走」这件
 * 事正好能用来判断报告是不是真的分享出去了：分享面板自己不会把「用户选了
 * 哪个 App」告诉我们，只有这个信号靠得住。
 */
class ExportFileProvider : FileProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val descriptor = super.openFile(uri, mode)
        if (ReportAccess.isSharedReportRead(mode, uri.lastPathSegment)) {
            scope.launch { ExportStamp.mark(requireNotNull(context), ExportAction.Share) }
        }
        return descriptor
    }
}
