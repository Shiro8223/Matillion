package com.matillion.techtest2025.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.service.DataAnalysisService;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisReportController {

    private final DataAnalysisService dataAnalysisService;

    @GetMapping(value = "/{id}/report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getReport(@PathVariable("id") long id) {

        // Load dataset summary
        DataAnalysisResponse analysis = dataAnalysisService.getAnalysisById(id);

        // fetch enhanced stats (types, dq, etc.)
        var stats = dataAnalysisService.getStats(id);

        // index by lowercased column name for quick lookup
        java.util.Map<String, com.matillion.techtest2025.controller.response.StatsResponse.ColumnStatsView> byName = new java.util.HashMap<>();
        if (stats != null && stats.columns() != null) {
            for (var v : stats.columns()) {
                byName.put(v.columnName().toLowerCase(), v);
            }
        }

        // Build column rows
        StringBuilder rows = new StringBuilder();
        var cols = (analysis.columnStatistics() != null)
                ? analysis.columnStatistics()
                : java.util.List.<com.matillion.techtest2025.model.ColumnStatistics>of();

        for (var cs : cols) {
            var view = byName.get(cs.columnName().toLowerCase());

            String dtype = (view != null && view.dataType() != null) ? view.dataType() : "-";

            // DQ pill (score + grade)
            String grade = (view != null && view.qualityGrade() != null)
                    ? view.qualityGrade().toUpperCase(java.util.Locale.ROOT)
                    : null;

            String dqHtml = "-";
            if (view != null && view.qualityScore() != null && grade != null) {
                dqHtml = "<span class='pill " + grade + "'>" + view.qualityScore() + " (" + grade + ")</span>";
            }

            // --- outlier info (numeric only) ---
            String outlierDisplay = "-";
            var out = (view != null) ? view.outlierSummary() : null;

            if (out != null && out.outlierCount() != null) {
                String lowerTxt = (out.lowerFence() == null) ? "-" : String.valueOf(out.lowerFence());
                String upperTxt = (out.upperFence() == null) ? "-" : String.valueOf(out.upperFence());
                outlierDisplay = "<span title='Lower: " + lowerTxt + "  Upper: " + upperTxt + "'>"
                        + "&#9650; " + out.outlierCount() + "</span>";
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

        // Build final HTML
        String html = "<!doctype html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='utf-8'>" +
                "<title>Dataset Report</title>" +
                "<style>" +
                "  :root{--bg:#fff;--muted:#666;--line:#eee;--pill:#eef;--pill-b:#e8f2ff;--pill-c:#fff7d6;--pill-d:#ffe6d9;--pill-f:#ffdcdc}"
                +
                "  *{box-sizing:border-box}" +
                "  body{margin:0;background:var(--bg);color:#111;font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;line-height:1.45}"
                +
                "  .container{max-width:1100px;margin:24px auto;padding:0 16px}" +
                "  h1{margin:0 0 8px 0;font-size:28px}" +
                "  .muted{color:var(--muted)}" +
                "  /* summary cards */" +
                "  .grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin:16px 0 24px}" +
                "  .card{border:1px solid var(--line);border-radius:12px;padding:16px;background:#fff;box-shadow:0 1px 2px rgba(0,0,0,.04)}"
                +
                "  .k{font-size:12px;color:var(--muted);margin-bottom:6px} .v{font-size:22px;font-weight:600}" +
                "  /* table */" +
                "  .table-wrap{overflow:auto;border:1px solid var(--line);border-radius:12px}" +
                "  table{width:100%;border-collapse:separate;border-spacing:0;background:#fff}" +
                "  th,td{padding:12px 14px;border-top:1px solid var(--line);vertical-align:middle}" +
                "  th:first-child,td:first-child{padding-left:18px} th:last-child,td:last-child{padding-right:18px}" +
                "  thead th{position:sticky;top:0;background:#fafafa;border-top:0;border-bottom:1px solid var(--line);z-index:1}"
                +
                "  tbody tr:nth-child(odd){background:#fcfcfc}" +
                "  tbody tr:hover{background:#f7faff}" +
                "  td.num{text-align:right;font-variant-numeric:tabular-nums}" +
                "  /* badges */" +
                "  .pill{display:inline-block;padding:2px 8px;border-radius:999px;border:1px solid #ccd;font-size:12px;background:var(--pill)}"
                +
                "  .pill.A{background:var(--pill-b)} .pill.B{background:var(--pill-b)}" +
                "  .pill.C{background:var(--pill-c)} .pill.D{background:var(--pill-d)} .pill.F{background:var(--pill-f)}"
                +
                "  /* little ▲ tooltip cursor */ td span[title]{cursor:help}" +
                "  /* responsive */" +
                "  @media (max-width:960px){.grid{grid-template-columns:1fr}.v{font-size:20px}}" +
                "</style>" +

                "</head>" +
                "<body><div class='container'>" +
                "<header>" +
                "<h1>Dataset Report</h1>" +
                "<p class='muted'>Analysis ID: " + id + "</p>" +
                "</header>" +

                "<section class='grid'>" +
                "  <div class='card'><div class='k'>Rows</div><div class='v'>" + analysis.numberOfRows()
                + "</div></div>" +
                "  <div class='card'><div class='k'>Columns</div><div class='v'>" + analysis.numberOfColumns()
                + "</div></div>" +
                "  <div class='card'><div class='k'>Created</div><div class='v'>" + analysis.createdAt()
                + "</div></div>" +
                "</section>" +

                "<section>" +
                "<h2 style='margin-top:24px'>Columns</h2>" +
                "<div class='table-wrap'>" +
                "<table aria-label='Column statistics'>" +
                "<thead><tr><th style='width:48%'>Name</th><th style='width:14%'>Type</th><th>Nulls</th><th>Uniques</th><th>DQ</th><th>Outliers</th></tr></thead>"
                +
                "<tbody>" + rows + "</tbody>" +
                "</table>" +
                "</div>" +
                "<p class='muted' style='margin-top:8px'>View raw JSON: " +
                "<a href='/api/analysis/" + id + "/stats'>/api/analysis/" + id + "/stats</a>" +
                "</p>" +
                "</section>" +
                "</div></body></html>";

        return ResponseEntity.ok(html);
    }

    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
