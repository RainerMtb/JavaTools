# Utilities written in Java and JavaFX

Small projects only using Standard Java and JavaFX. All code is contained in one single source file.

## RawViewer

Open and play video files stored in raw file formats like YUV444P, NV12, BGR24, etc.

![RawViewer Screenshot](doc/ScreenshotRawViewer.jpg)

## DataPlotter

InteractiveLineChart is an extension to LineChart which makes the chart behave like a map that can be zoomed and panned to inspect data points in close detail.

Main Features
- drag the chart area to adjust the visible range for both axes
- scroll the mouse wheel on the chart area to zoom in and out on both axes
- place the mouse on one individual axis to adjust only that specific axis

Use the SeriesBuilder subclass via ```seriesBuilder()``` to conveniently add data

Use right click context menu on the chart area to
- set axes to equal data intervals
- enable or disable axes auto ranging
- show or hide symbols on data points
- and more

Have a look at the Demo Application

![DataPlotter Screenshot](doc/ScreenshotDataPlotter.jpg)
![DataPlotter Animation](doc/AnimationDataPlotter.gif)
