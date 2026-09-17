package com.worklog.quickrecord.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worklog.quickrecord.R
import com.worklog.quickrecord.domain.Record
import com.worklog.quickrecord.ui.LocalPhoto
import com.worklog.quickrecord.ui.icons.BackIcon
import com.worklog.quickrecord.ui.theme.LocalAppColors
import com.worklog.quickrecord.util.DisplayFormat

@Composable
fun RecordDetailScreen(
    record: Record?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Column(modifier = modifier.fillMaxSize().background(colors.page)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BackIcon,
                    contentDescription = stringResource(R.string.action_back),
                    tint = colors.ink,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.screen_record_detail),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = colors.ink,
            )
        }

        if (record == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.empty_deleted),
                    color = colors.sub,
                    fontSize = 14.sp,
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.card)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = record.place,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    color = colors.ink,
                )
                Text(
                    text = DisplayFormat.full(record.occurredAt),
                    fontSize = 13.sp,
                    color = colors.sub,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider),
                )
                Text(
                    text = record.description,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = colors.ink,
                )
            }

            record.photos.forEach { photo ->
                val ratio = if (photo.height > 0) {
                    photo.width.toFloat() / photo.height.toFloat()
                } else {
                    4f / 3f
                }
                LocalPhoto(
                    relativePath = photo.relativePath,
                    maxSize = 1080,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio.coerceIn(0.6f, 2.2f))
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, colors.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Text(stringResource(R.string.action_edit), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = onDelete,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, colors.danger),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Text(stringResource(R.string.action_delete), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
