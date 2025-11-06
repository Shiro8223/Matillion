package com.matillion.techtest2025.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.controller.response.StatsResponse;
import com.matillion.techtest2025.model.ColumnStatistics;
import com.matillion.techtest2025.service.DataAnalysisService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisReportController {

    private final DataAnalysisService dataAnalysisService;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    // -------------------------------------------------
    // HTML preview page (browser view)
    // GET /api/analysis/{id}/report
    // -------------------------------------------------
    @GetMapping(value = "/{id}/report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getReport(@PathVariable("id") long id) {
        return ResponseEntity.ok(buildReportSkeleton(false, id)); // HTML5 for browser
    }

    // -------------------------------------------------
    // PDF download
    // GET /api/analysis/{id}/report?format=pdf
    // -------------------------------------------------
    @GetMapping(value = "/{id}/report", params = "format=pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReportPdf(@PathVariable("id") long id) {
        String xhtml = buildReportSkeleton(true, id); // XHTML for openhtmltopdf

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(baos);
            builder.run();

            String filename = "analysis-" + id + "-report.pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            String msg = "Failed to generate PDF: " + e.getMessage();
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    // -------------------------------------------------
    // JSON download ONLY (no conflict with DataAnalysisController#stats)
    // GET /api/analysis/{id}/stats?download=true
    // -------------------------------------------------
    @GetMapping(value = "/{id}/stats", params = "download=true", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> downloadStats(@PathVariable("id") long id) {
        StatsResponse stats = dataAnalysisService.getStats(id);
        try {
            byte[] bytes = mapper.writeValueAsBytes(stats);
            String filename = "analysis-" + id + "-stats.json";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bytes);
        } catch (Exception e) {
            String msg = "Failed to serialize JSON: " + e.getMessage();
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    // =================================================
    // Shared HTML/XHTML builder
    // =================================================
    private String buildReportSkeleton(boolean xhtml, long id) {
        DataAnalysisResponse analysis = dataAnalysisService.getAnalysisById(id);
        StatsResponse stats = dataAnalysisService.getStats(id);

        Map<String, StatsResponse.ColumnStatsView> byName = new HashMap<>();
        if (stats != null && stats.columns() != null) {
            for (var v : stats.columns()) {
                byName.put(v.columnName().toLowerCase(), v);
            }
        }

        StringBuilder rows = new StringBuilder();
        List<ColumnStatistics> cols = (analysis.columnStatistics() != null) ? analysis.columnStatistics() : List.of();

        for (var cs : cols) {
            var view = byName.get(cs.columnName().toLowerCase());
            String dtype = (view != null && view.dataType() != null) ? view.dataType() : "-";

            String grade = (view != null && view.qualityGrade() != null)
                    ? view.qualityGrade().toUpperCase(java.util.Locale.ROOT)
                    : null;

            String dqHtml = "-";
            if (view != null && view.qualityScore() != null && grade != null) {
                dqHtml = "<span class='pill " + grade + "'>" + view.qualityScore() + " (" + grade + ")</span>";
            }

            String outlierDisplay = "-";
            var out = (view != null) ? view.outlierSummary() : null;
            if (out != null && out.outlierCount() != null) {
                String lowerTxt = (out.lowerFence() == null) ? "-" : String.valueOf(out.lowerFence());
                String upperTxt = (out.upperFence() == null) ? "-" : String.valueOf(out.upperFence());
                outlierDisplay = "<span title='Lower: " + lowerTxt + "  Upper: " + upperTxt + "'>&#9650; "
                        + out.outlierCount() + "</span>";
            }

            rows.append("<tr>")
                    .append("<td>").append(escape(cs.columnName())).append("</td>")
                    .append("<td>").append(escape(dtype)).append("</td>")
                    .append("<td class='num'>").append(cs.nullCount()).append("</td>")
                    .append("<td class='num'>").append(cs.uniqueCount()).append("</td>")
                    .append("<td class='num'>").append(dqHtml).append("</td>")
                    .append("<td class='num'>").append(outlierDisplay).append("</td>")
                    .append("</tr>");

        }

        String openTag = xhtml
                ? "<html xmlns='http://www.w3.org/1999/xhtml' lang='en'>"
                : "<!doctype html><html lang='en'>";
        String meta = xhtml ? "<meta charset='utf-8' />" : "<meta charset='utf-8'>";

        return openTag +
                "<head>" + meta +
                "<title>Dataset Report</title>" +
                "<style>" +
                ":root{--bg:#fff;--muted:#666;--line:#eee;--pill:#eef;--pill-b:#e8f2ff;--pill-c:#fff7d6;--pill-d:#ffe6d9;--pill-f:#ffdcdc}"
                +
                "*{box-sizing:border-box}" +
                "body{margin:0;background:var(--bg);color:#111;font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;line-height:1.45}"
                +
                ".container{max-width:1100px;margin:24px auto;padding:0 16px}" +
                "h1{margin:0 0 8px 0;font-size:28px}" +
                ".muted{color:var(--muted)}" +
                ".grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin:16px 0 24px}" +
                ".card{border:1px solid var(--line);border-radius:12px;padding:16px;background:#fff;box-shadow:0 1px 2px rgba(0,0,0,.04)}"
                +
                ".k{font-size:12px;color:var(--muted);margin-bottom:6px}.v{font-size:22px;font-weight:600}" +
                ".table-wrap{overflow:auto;border:1px solid var(--line);border-radius:12px}" +
                "table{width:100%;border-collapse:separate;border-spacing:0;background:#fff}" +
                "th,td{padding:12px 14px;border-top:1px solid var(--line);vertical-align:middle}" +
                "th:first-child,td:first-child{padding-left:18px} th:last-child,td:last-child{padding-right:18px}" +
                "thead th{position:sticky;top:0;background:#fafafa;border-top:0;border-bottom:1px solid var(--line);z-index:1}"
                +
                "tbody tr:nth-child(odd){background:#fcfcfc}" +
                "tbody tr:hover{background:#f7faff}" +
                "td.num{text-align:right;font-variant-numeric:tabular-nums}" +
                ".pill{display:inline-block;padding:2px 8px;border-radius:999px;border:1px solid #ccd;font-size:12px;background:var(--pill)}"
                +
                ".pill.A{background:var(--pill-b)} .pill.B{background:var(--pill-b)} .pill.C{background:var(--pill-c)} .pill.D{background:var(--pill-d)} .pill.F{background:var(--pill-f)}"
                +
                ".actions{display:flex;gap:10px;margin:18px 0}" +
                ".btn{display:inline-block;padding:8px 12px;border-radius:10px;border:1px solid var(--line);background:#f7f8ff;text-decoration:none;color:#111;font-weight:600}"
                +
                ".btn:hover{background:#eef2ff}" +
                "@media (max-width:960px){.grid{grid-template-columns:1fr}.v{font-size:20px}}" +
                "</style></head>" +

                "<body><div class='container'>" +
                "<header><h1>Dataset Report</h1><p class='muted'>Analysis ID: " + id + "</p></header>" +

                "<section class='grid'>" +
                "<div class='card'><div class='k'>Rows</div><div class='v'>" + analysis.numberOfRows() + "</div></div>"
                +
                "<div class='card'><div class='k'>Columns</div><div class='v'>" + analysis.numberOfColumns()
                + "</div></div>" +
                "<div class='card'><div class='k'>Created</div><div class='v'>" + analysis.createdAt() + "</div></div>"
                +
                "</section>" +

                "<section>" +
                "<h2 style='margin-top:24px'>Columns</h2>" +
                "<div class='table-wrap'>" +
                "<table aria-label='Column statistics'>" +
                "<thead><tr><th style='width:48%'>Name</th><th style='width:14%'>Type</th><th>Nulls</th><th>Uniques</th><th>DQ</th><th>Outliers</th></tr></thead>"
                +
                "<tbody>" + rows + "</tbody>" +
                "</table></div>" +

                "<p class='muted' style='margin-top:8px'>View raw JSON: " +
                "<a href='/api/analysis/" + id + "/stats'>/api/analysis/" + id + "/stats</a></p>" +

                "<div class='actions'>" +
                "<a class='btn' href='/api/analysis/" + id + "/stats?download=true'>Download JSON</a>" +
                "<a class='btn' href='/api/analysis/" + id + "/report?format=pdf'>Download PDF</a>" +
                "</div>" +
                "</section>" +
                "</div></body></html>";
    }

    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
