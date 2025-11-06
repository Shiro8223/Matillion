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

        // Build column rows
        StringBuilder rows = new StringBuilder();
        var cols = (analysis.columnStatistics() != null)
                ? analysis.columnStatistics()
                : java.util.List.<com.matillion.techtest2025.model.ColumnStatistics>of();

        for (var cs : cols) {
            rows.append("<tr>")
                    .append("<td>").append(escape(cs.columnName())).append("</td>")
                    .append("<td style=\"text-align:right\">").append(cs.nullCount()).append("</td>")
                    .append("<td style=\"text-align:right\">").append(cs.uniqueCount()).append("</td>")
                    .append("</tr>");
        }

        // Build final HTML
        String html = "<!doctype html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='utf-8'>" +
                "<title>Dataset Report</title>" +
                "<style>" +
                "body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;margin:24px;line-height:1.4}"
                +
                ".grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-top:12px}" +
                ".card{border:1px solid #eee;border-radius:12px;padding:16px}" +
                ".muted{color:#666}" +
                ".k{font-size:12px;color:#666;margin-bottom:4px}" +
                ".v{font-size:20px;font-weight:600}" +
                "a{text-decoration:none}" +
                "a:hover{text-decoration:underline}" +
                "table{width:100%;border-collapse:separate;border-spacing:0;margin-top:20px}" +
                "th,td{padding:10px 12px;border-top:1px solid #eee}" +
                "th{font-weight:600;text-align:left;background:#fafafa;border-top:0;border-bottom:1px solid #eee}" +
                "tr:last-child td{border-bottom:1px solid #eee}" +
                "td:nth-child(2), td:nth-child(3){text-align:right}" +
                "</style>" +
                "</head>" +
                "<body>" +
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
                "<table aria-label='Column statistics'>" +
                "<thead><tr><th style='width:60%'>Name</th><th>Nulls</th><th>Uniques</th></tr></thead>" +
                "<tbody>" + rows + "</tbody>" +
                "</table>" +
                "<p class='muted' style='margin-top:8px'>View raw JSON: " +
                "<a href='/api/analysis/" + id + "/stats'>/api/analysis/" + id + "/stats</a>" +
                "</p>" +
                "</section>" +
                "</body></html>";

        return ResponseEntity.ok(html);
    }

    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
