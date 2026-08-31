package com.qiji.cps.module.cps.service.refund;

import com.qiji.cps.framework.excel.core.util.ExcelUtils;
import com.qiji.cps.module.cps.dal.dataobject.order.CpsOrderDO;
import com.qiji.cps.module.cps.dal.dataobject.refund.CpsRefundReportDetailDO;
import com.qiji.cps.module.cps.dal.dataobject.refund.CpsRefundReportImportDO;
import com.qiji.cps.module.cps.dal.mysql.order.CpsOrderMapper;
import com.qiji.cps.module.cps.dal.mysql.refund.CpsRefundReportDetailMapper;
import com.qiji.cps.module.cps.dal.mysql.refund.CpsRefundReportImportMapper;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import cn.idev.excel.annotation.ExcelProperty;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Validated
public class CpsRefundReportImportServiceImpl implements CpsRefundReportImportService {
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;
    @Resource private CpsRefundReportImportMapper importMapper;
    @Resource private CpsRefundReportDetailMapper detailMapper;
    @Resource private CpsOrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpsRefundReportImportResult importReport(MultipartFile file, String platformCode,
                                                    String vendorCode, String reportPeriod) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("退款报表文件不能为空");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("退款报表文件不能超过20MB");
        if (!StringUtils.hasText(platformCode)) throw new IllegalArgumentException("平台编码不能为空");
        byte[] content = file.getBytes();
        String hash = sha256(content);
        String source = "DATAOKE";
        CpsRefundReportImportDO existing = importMapper.selectByFileHash(source, hash);
        if (existing != null) return new CpsRefundReportImportResult(existing, true);
        String batchNo = "refund-" + hash.substring(0, 16);
        CpsRefundReportImportDO report = CpsRefundReportImportDO.builder()
                .batchNo(batchNo).source(source).fileName(file.getOriginalFilename()).fileHash(hash)
                .status("PROCESSING").totalRows(0).matchedRows(0).diffRows(0).build();
        try {
            importMapper.insert(report);
        } catch (DuplicateKeyException duplicate) {
            CpsRefundReportImportDO concurrent = importMapper.selectByFileHash(source, hash);
            if (concurrent != null) return new CpsRefundReportImportResult(concurrent, true);
            throw duplicate;
        }
        try {
            List<RefundRow> rows = parse(file, content);
                int matched = 0, differences = 0;
                for (RefundRow row : rows) {
                if (!StringUtils.hasText(row.platformOrderId)) { continue; }
                CpsOrderDO order = orderMapper.selectByPlatformOrderId(platformCode, row.platformOrderId.trim());
                String matchStatus = order == null ? "ORDER_NOT_FOUND" : "MATCHED";
                if (order != null) matched++; else differences++;
                if (detailMapper.selectByImportAndOrder(report.getId(), platformCode, row.platformOrderId.trim()) == null) {
                    detailMapper.insert(CpsRefundReportDetailDO.builder().importId(report.getId())
                            .platformCode(platformCode).platformOrderId(row.platformOrderId.trim())
                            .refundType(row.refundType).refundAmount(row.refundAmount).refundTime(row.refundTime)
                            .orderId(order == null ? null : order.getId()).matchStatus(matchStatus)
                            .differenceReason(order == null ? "本地订单不存在" : null).build());
                }
            }
            report.setTotalRows(rows.size()); report.setMatchedRows(matched); report.setDiffRows(differences); report.setStatus("SUCCESS");
        } catch (Exception ex) {
            report.setStatus("FAILED"); report.setFailureReason(ex.getMessage() == null ? "解析失败" : ex.getMessage());
        }
        importMapper.updateById(report);
        return new CpsRefundReportImportResult(report, false);
    }

    @Override
    public CpsRefundReportImportDO getReport(Long id) {
        CpsRefundReportImportDO report = importMapper.selectById(id);
        if (report == null) throw new IllegalArgumentException("退款报表导入批次不存在: " + id);
        return report;
    }

    @Override
    public List<CpsRefundReportDetailDO> getDetails(Long id) {
        getReport(id);
        return detailMapper.selectListByImportId(id);
    }

    private List<RefundRow> parse(MultipartFile file, byte[] content) throws IOException {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv") || name.endsWith(".txt")) return parseCsv(new String(content, StandardCharsets.UTF_8));
        return ExcelUtils.read(file, RefundRow.class);
    }

    private List<RefundRow> parseCsv(String text) {
        List<RefundRow> result = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (int i = 1; i < lines.length; i++) {
            String[] c = lines[i].split(",", -1); if (c.length == 0) continue;
            RefundRow row = new RefundRow(); row.platformOrderId = at(c,0); row.refundType = at(c,1);
            row.refundAmount = decimal(at(c,2)); row.commissionAmount = decimal(at(c,3));
            row.platformStatus = at(c,4); row.rawSummary = lines[i]; result.add(row);
        }
        return result;
    }
    private String at(String[] a, int i) { return i < a.length ? a[i].trim() : null; }
    private BigDecimal decimal(String v) { try { return StringUtils.hasText(v) ? new BigDecimal(v) : null; } catch (Exception e) { return null; } }
    private String sha256(byte[] bytes) {
        try { byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes); StringBuilder b = new StringBuilder(); for (byte x : digest) b.append(String.format("%02x", x)); return b.toString(); }
        catch (Exception e) { throw new IllegalStateException("无法计算文件摘要", e); }
    }

    @Data
    public static class RefundRow {
        @ExcelProperty(index = 0) private String platformOrderId;
        @ExcelProperty(index = 1) private String refundType;
        @ExcelProperty(index = 2) private BigDecimal refundAmount;
        @ExcelProperty(index = 3) private BigDecimal commissionAmount;
        @ExcelProperty(index = 4) private String platformStatus;
        private LocalDateTime refundTime;
        private String rawSummary;
    }
}
