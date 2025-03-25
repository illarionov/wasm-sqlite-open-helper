import { createClassFromSpec } from "react-vega";

const spec = {
  "$schema": "https://vega.github.io/schema/vega-lite/v5.json",
  "data": {"name": "values" },
  "facet": {
    "row": {
      "field": "test",
      "type": "nominal",
      "title": "SQLite Driver tests",
      "sort": [ "create_database", "select_with_paging", "huge_select" ],
      "header": {
        "orient": "top",
        "labelAnchor": "start"
      }
    }
  },
  "resolve": {
    "axis": {"x": "independent", "y": "independent"},
    "scale": { "x": "independent" }
  },
  "spec": {
    "width": 600,
    "transform": [
      {"calculate": "datum.minimum/1000000000", "as": "minimumSec" },
      {"calculate": "datum.maximum/1000000000", "as": "maximumSec"},
      {"calculate": "datum.median/1000000000", "as": "medianSec"}
    ],
    "encoding": {
      "x": { "field": "medianSec", "title": "Median time, sec", "type": "quantitative" },
      "y": {
        "field": "driver",
        "type": "ordinal",
        "axis": null,
        "sort": ["Android", "Bundled", "ChicoryAot", "ChicoryAotBA", "Chasm", "ChasmNoFusion", "Chicory", "ChicoryBA"]
      },
      "color": {
        "field": "driver",
        "scale": {
            "domain": ["Android", "Bundled",  "ChicoryAot", "ChicoryAotBA", "Chicory", "ChicoryBA", "Chasm", "ChasmNoFusion"],
            "range": [ "#2ca02c", "#d62728", "#3182bd", "#9ecae1", "#17becf", "#9edae5", "#8c6d31", "#e7ba52" ]
        },
      }
    },
    "layer": [
      {
        "mark": { "type": "bar" },
        "encoding": {
          "color": { "field": "driver", "title": "Driver" }
        }
      },
      {
        "mark": { "type": "errorbar", "ticks": true, "size": 4 },
        "encoding": {
          "x": { "field": "maximumSec", "type": "quantitative" },
          "x2": {"field": "minimumSec"},
          "color": { "value": "black" }
        }
      },
      {
        "mark": { "type": "point", "filled": true },
        "encoding": { "color": { "value": "black" }}
      }
    ]
  }
};

const SQLiteDriverBenchmarkReport = createClassFromSpec({
  mode: 'vega-lite',
  spec
});

export default SQLiteDriverBenchmarkReport;
