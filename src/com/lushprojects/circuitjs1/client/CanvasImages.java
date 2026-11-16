package com.lushprojects.circuitjs1.client;

import java.util.function.Consumer;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.lushprojects.circuitjs1.client.util.Locale;

final class CanvasImages {
    private final CirSim sim;
    private PopupPanel menuPopup;
    private Dialog settingsDialog;

    CanvasImages(CirSim sim, CanvasElement canvas) {
        this.sim = sim;
        install(canvas);
    }

    private void options(JavaScriptObject image, int x, int y) {
        if (sim.dialogIsShowing())
            return;
        MenuBar menu = new MenuBar(true);
        boolean locked = isLocked(image);
        menu.addItem(Locale.LS(locked ? "Unlock" : "Lock"), () -> {
            menuPopup.hide();
            update(image, "locked", locked ? 0 : 1, 0);
        });
        if (locked) {
            menuPopup = sim.showComponentTools(menu, x, y);
            return;
        }
        menu.addItem(Locale.LS("Opacity..."), () -> {
            menuPopup.hide();
            showSettings("Opacity", new String[] {"Opacity (%)"},
                new int[] {(int) Math.round(value(image, "opacity") * 100)}, 0, 100,
                values -> update(image, "opacity", values[0], 0), null, null);
        });
        menu.addItem(Locale.LS("Resize"), () -> {
            menuPopup.hide();
            resize(image);
        });
        addRotation(menu, image, "Rotate X", "X tilt (degrees)", "rx");
        addRotation(menu, image, "Rotate Y", "Y tilt (degrees)", "ry");
        addRotation(menu, image, "Rotate XY", "Canvas rotation (degrees)", "rz");
        menu.addItem(Locale.LS("Grid..."), () -> {
            menuPopup.hide();
            int rows = (int) value(image, "rows"), columns = (int) value(image, "columns");
            double width = value(image, "w"), height = value(image, "h");
            double longest = Math.max(width, height);
            int[] automatic = {
                Math.max(1, (int) Math.round(8 * height / longest)),
                Math.max(1, (int) Math.round(8 * width / longest))
            };
            showSettings("Grid", new String[] {"Rows", "Columns"},
                rows == 0 ? automatic : new int[] {rows, columns}, 1, 100,
                values -> update(image, "grid", values[0], values[1]), automatic,
                rows == 0 ? null : () -> update(image, "grid", 0, 0));
        });
        menu.addItem(Locale.LS("Delete"), () -> {
            menuPopup.hide();
            update(image, "delete", 0, 0);
        });
        menuPopup = sim.showComponentTools(menu, x, y);
    }

    private void addRotation(MenuBar menu, JavaScriptObject image, String title, String label, String field) {
        menu.addItem(Locale.LS(title) + "...", () -> {
            menuPopup.hide();
            showSettings(title, new String[] {label}, new int[] {(int) value(image, field)}, -360, 360,
                values -> update(image, field, values[0], 0), null, null);
        });
    }

    private void showSettings(String title, String[] labels, int[] initial, int min, int max,
            Consumer<int[]> save, int[] automatic, Runnable remove) {
        VerticalPanel panel = new VerticalPanel();
        panel.setSpacing(8);
        IntegerBox[] inputs = new IntegerBox[labels.length];
        for (int i = 0; i < labels.length; i++) {
            String label = Locale.LS(labels[i]);
            panel.add(new Label(label));
            IntegerBox input = new IntegerBox();
            input.setStyleName("gwt-TextBox");
            input.getElement().setAttribute("type", "number");
            input.getElement().setAttribute("aria-label", label);
            input.setWidth("8em");
            input.setValue(initial[i]);
            inputs[i] = input;
            panel.add(input);
        }
        Label error = new Label();
        error.getElement().setAttribute("aria-live", "polite");
        error.setVisible(false);
        panel.add(error);
        Dialog dialog = new Dialog() {
            @Override
            public void enterPressed() {
                int[] values = new int[inputs.length];
                for (int i = 0; i < inputs.length; i++) {
                    Integer value = inputs[i].getValue();
                    if (value == null || value < min || value > max) {
                        error.setText(Locale.LS("Enter a whole number between") + " " + min + " "
                            + Locale.LS("and") + " " + max + ".");
                        error.setVisible(true);
                        inputs[i].setFocus(true);
                        return;
                    }
                    values[i] = value;
                }
                save.accept(values);
                closeDialog();
            }
        };
        dialog.setText(Locale.LS(title));
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(4);
        buttons.add(new Button(Locale.LS("Apply"), (ClickHandler) event -> dialog.enterPressed()));
        if (automatic != null) {
            buttons.add(new Button(Locale.LS("Auto"), (ClickHandler) event -> {
                for (int i = 0; i < inputs.length; i++)
                    inputs[i].setValue(automatic[i]);
                error.setVisible(false);
            }));
        }
        buttons.add(new Button(Locale.LS("Cancel"), (ClickHandler) event -> dialog.closeDialog()));
        panel.add(buttons);
        if (remove != null) {
            panel.add(new Button(Locale.LS("Remove grid"), (ClickHandler) event -> {
                remove.run();
                dialog.closeDialog();
            }));
        }
        settingsDialog = dialog;
        dialog.setWidget(panel);
        CirSim.dialogShowing = dialog;
        dialog.center();
    }

    private native boolean isLocked(JavaScriptObject image) /*-{
        return !!image.locked;
    }-*/;

    private native double value(JavaScriptObject image, String field) /*-{
        return image[field];
    }-*/;

    private native void update(JavaScriptObject image, String field, int first, int second) /*-{
        this.update(image, field, first, second);
    }-*/;

    private native void resize(JavaScriptObject image) /*-{
        this.resize(image);
    }-*/;

    private native void install(CanvasElement canvas) /*-{
        var owner = this;
        var sim = this.@com.lushprojects.circuitjs1.client.CanvasImages::sim;
        var doc = $doc;
        var images = [], selected = null, resizing = false, gesture = null;
        var generation = 0, touchOwned = false, suppressClick = false;
        var corners = [[-1, -1], [1, -1], [1, 1], [-1, 1]];
        var view = {scale: 1, x: 0, y: 0, width: 0, height: 0, top: 0};
        var picker = doc.createElement("input");
        picker.type = "file";
        picker.accept = "image/*";
        picker.hidden = true;
        doc.body.appendChild(picker);

        function repaint() {
            sim.@com.lushprojects.circuitjs1.client.CirSim::repaint()();
        }
        function snapshot() {
            sim.@com.lushprojects.circuitjs1.client.CirSim::pushUndo()();
        }
        function changed() {
            sim.@com.lushprojects.circuitjs1.client.CirSim::canvasImageChanged()();
        }
        function radians(angle) {
            return angle * Math.PI / 180;
        }
        // Orthographic tilt about X and Y, followed by a rotation in the canvas plane.
        function matrix(image) {
            var x = radians(image.rx), y = radians(image.ry), z = radians(image.rz);
            function visibleCosine(angle) {
                var cosine = Math.cos(angle);
                return Math.abs(cosine) >= .08 ? cosine : (cosine < 0 ? -.08 : .08);
            }
            var cosX = visibleCosine(x), cosY = visibleCosine(y);
            var shear = Math.sin(x) * Math.sin(y);
            var c = Math.cos(z), s = Math.sin(z);
            return [c * cosY, s * cosY, c * shear - s * cosX, s * shear + c * cosX];
        }
        function project(m, x, y) {
            return [m[0] * x + m[2] * y, m[1] * x + m[3] * y];
        }
        function unproject(m, x, y) {
            var determinant = m[0] * m[3] - m[1] * m[2];
            return [(m[3] * x - m[2] * y) / determinant, (m[0] * y - m[1] * x) / determinant];
        }
        function select(image, showHandles) {
            selected = image;
            resizing = showHandles;
            repaint();
        }
        function loadSource(image) {
            image.full = new $wnd.Image();
            image.full.onload = function() {
                var preview = doc.createElement("canvas");
                var scale = Math.min(1, 192 / Math.max(image.full.naturalWidth, image.full.naturalHeight));
                preview.width = Math.max(1, Math.round(image.full.naturalWidth * scale));
                preview.height = Math.max(1, Math.round(image.full.naturalHeight * scale));
                preview.getContext("2d").drawImage(image.full, 0, 0, preview.width, preview.height);
                image.preview = preview;
                repaint();
            };
            image.full.src = image.source;
        }
        owner.choose = function() {
            picker.value = "";
            picker.click();
        };
        picker.addEventListener("change", function() {
            var file = picker.files[0];
            if (!file) return;
            var current = generation;
            function failed() {
                if (current === generation) $wnd.alert("This image could not be opened.");
            }
            var reader = new $wnd.FileReader();
            reader.onerror = failed;
            reader.onload = function() {
                if (current !== generation) return;
                var source = reader.result;
                var probe = new $wnd.Image();
                probe.onerror = failed;
                probe.onload = function() {
                    if (current !== generation) return;
                    if (!probe.naturalWidth || !probe.naturalHeight) {
                        failed();
                        return;
                    }
                    var scale = Math.min(1, view.width * .62 / (view.scale * probe.naturalWidth),
                        view.height * .62 / (view.scale * probe.naturalHeight));
                    var image = {
                        source: source, x: (view.width / 2 - view.x) / view.scale,
                        y: (view.top + view.height / 2 - view.y) / view.scale,
                        w: probe.naturalWidth * scale, h: probe.naturalHeight * scale,
                        opacity: 1, rx: 0, ry: 0, rz: 0, rows: 0, columns: 0
                    };
                    snapshot();
                    images.push(image);
                    loadSource(image);
                    select(image, true);
                    changed();
                };
                // Some file providers supply a generic MIME type even for a valid raster image.
                source = source.replace(/^data:(?:application\/octet-stream)?;/i, "data:image/png;");
                if (!/^data:image\/[a-z0-9.+-]+;base64,/i.test(source)) {
                    failed();
                    return;
                }
                probe.src = source;
            };
            reader.readAsDataURL(file);
        });
        owner.clear = function() {
            generation++;
            if (gesture) $wnd.clearTimeout(gesture.timer);
            gesture = null;
            images = [];
            select(null, false);
        };
        owner.dump = function() {
            return images.map(function(image) {
                var data = [image.source, image.x, image.y, image.w, image.h,
                    image.opacity, image.rx, image.ry, image.rz, image.rows, image.columns];
                if (image.locked) data.push(true);
                return "I " + JSON.stringify(data) + "\n";
            }).join("");
        };
        owner.restore = function(line) {
            try {
                var data = JSON.parse(line);
                if (!Array.isArray(data) || (data.length !== 11 && (data.length !== 12 || data[11] !== true)) ||
                    typeof data[0] !== "string" ||
                    !/^data:image\/[a-z0-9.+-]+;base64,[a-z0-9+\/=]+$/i.test(data[0]) ||
                    !data.slice(1, 11).every(function(value) { return typeof value === "number" && isFinite(value); }) ||
                    data[3] <= 0 || data[4] <= 0 || data[5] < 0 || data[5] > 1 ||
                    !data.slice(9, 11).every(function(value) {
                        return value >= 0 && value <= 100 && Math.floor(value) === value;
                    }) || (data[9] === 0) !== (data[10] === 0)) return false;
                var image = {source: data[0], x: data[1], y: data[2], w: data[3], h: data[4],
                    opacity: data[5], rx: data[6], ry: data[7], rz: data[8], rows: data[9], columns: data[10], locked: data[11] === true};
                images.push(image);
                loadSource(image);
                return true;
            } catch (error) {
                return false;
            }
        };
        owner.hasImages = function() { return images.length > 0; };
        owner.bounds = function() {
            if (!images.length) return null;
            var left = Infinity, top = Infinity, right = -Infinity, bottom = -Infinity;
            images.forEach(function(image) {
                var m = matrix(image);
                corners.forEach(function(corner) {
                    var p = project(m, corner[0] * image.w / 2, corner[1] * image.h / 2);
                    left = Math.min(left, image.x + p[0]);
                    top = Math.min(top, image.y + p[1]);
                    right = Math.max(right, image.x + p[0]);
                    bottom = Math.max(bottom, image.y + p[1]);
                });
            });
            left = Math.floor(left);
            top = Math.floor(top);
            return @com.lushprojects.circuitjs1.client.Rectangle::new(IIII)(
                left, top, Math.ceil(right) - left, Math.ceil(bottom) - top);
        };

        function columnLabel(index) {
            var label = "";
            for (index++; index > 0; index = Math.floor((index - 1) / 26)) {
                label = String.fromCharCode(65 + (index - 1) % 26) + label;
            }
            return label;
        }
        function drawGrid(ctx, image, x, y, scale) {
            var cellWidth = image.w / image.columns, cellHeight = image.h / image.rows;
            var col, row;
            ctx.beginPath();
            for (col = 0; col <= image.columns; col++) {
                ctx.moveTo(x + col * cellWidth, y);
                ctx.lineTo(x + col * cellWidth, y + image.h);
            }
            for (row = 0; row <= image.rows; row++) {
                ctx.moveTo(x, y + row * cellHeight);
                ctx.lineTo(x + image.w, y + row * cellHeight);
            }
            ctx.lineWidth = 1.25 / scale;
            ctx.setLineDash([.5 / scale, 4 / scale]);
            ctx.lineCap = "round";
            ctx.strokeStyle = "#fff";
            ctx.globalAlpha = .5;
            ctx.stroke();
        }
        function drawGridLabels(ctx, image, m, scale) {
            var fontSize = 13 / scale;
            ctx.save();
            ctx.font = fontSize + "px sans-serif";
            ctx.textAlign = "center";
            ctx.textBaseline = "middle";
            ctx.fillStyle = "#fff";
            ctx.strokeStyle = "#111";
            ctx.lineWidth = 2.5 / scale;
            function edgeLabel(text, x, y, tangentX, tangentY) {
                var p = project(m, x, y);
                var length = Math.hypot(tangentX, tangentY);
                var nx = -tangentY / length, ny = tangentX / length;
                if (nx * p[0] + ny * p[1] < 0) {
                    nx = -nx;
                    ny = -ny;
                }
                var halfWidth = ctx.measureText(text).width / 2;
                var gap = 4 / scale + Math.abs(nx) * halfWidth + Math.abs(ny) * fontSize / 2;
                var atX = image.x + p[0] + nx * gap, atY = image.y + p[1] + ny * gap;
                ctx.strokeText(text, atX, atY);
                ctx.fillText(text, atX, atY);
            }
            var cellWidth = image.w / image.columns, cellHeight = image.h / image.rows;
            var columnSpacing = cellWidth * Math.hypot(m[0], m[1]);
            var rowSpacing = cellHeight * Math.hypot(m[2], m[3]);
            var columnStep = Math.max(1, Math.ceil((ctx.measureText(columnLabel(image.columns - 1)).width
                + 3 / scale) / columnSpacing));
            var rowStep = Math.max(1, Math.ceil(fontSize / rowSpacing));
            for (var col = 0; col < image.columns; col += columnStep)
                edgeLabel(columnLabel(col), (col + .5) * cellWidth - image.w / 2, -image.h / 2, m[0], m[1]);
            for (var row = 0; row < image.rows; row += rowStep)
                edgeLabel(String(row + 1), -image.w / 2, (row + .5) * cellHeight - image.h / 2, m[2], m[3]);
            ctx.restore();
        }
        function clipToView(ctx) {
            ctx.beginPath();
            ctx.rect(-view.x / view.scale, (view.top - view.y) / view.scale,
                view.width / view.scale, view.height / view.scale);
            ctx.clip();
        }
        owner.draw = function(ctx, scale, x, y, top, width, height, interactive) {
            if (interactive) view = {scale: scale, x: x, y: y, top: top, width: width, height: height};
            if (!images.length) return;
            ctx.save();
            if (interactive) clipToView(ctx);
            images.forEach(function(image) {
                if (image.preview) {
                    var m = matrix(image);
                    ctx.save();
                    ctx.transform(m[0], m[1], m[2], m[3], image.x, image.y);
                    var source = interactive && gesture && gesture.image === image && gesture.moving ?
                        image.preview : image.full;
                    ctx.globalAlpha = image.opacity;
                    ctx.drawImage(source, -image.w / 2, -image.h / 2, image.w, image.h);
                    if (image.rows) drawGrid(ctx, image, -image.w / 2, -image.h / 2, scale);
                    ctx.restore();
                    if (image.rows) drawGridLabels(ctx, image, m, scale);
                }
            });
            ctx.restore();
        };
        owner.drawHandles = function(ctx) {
            if (!selected || !resizing) return;
            var m = matrix(selected), size = 12 / view.scale;
            ctx.save();
            clipToView(ctx);
            ctx.beginPath();
            corners.forEach(function(corner, index) {
                var p = project(m, corner[0] * selected.w / 2, corner[1] * selected.h / 2);
                var x = selected.x + p[0], y = selected.y + p[1];
                if (index === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            });
            ctx.closePath();
            ctx.lineWidth = 2 / view.scale;
            ctx.strokeStyle = "#29b6f6";
            ctx.stroke();
            ctx.fillStyle = "#fff";
            corners.forEach(function(corner) {
                var p = project(m, corner[0] * selected.w / 2, corner[1] * selected.h / 2);
                var x = selected.x + p[0] - size / 2, y = selected.y + p[1] - size / 2;
                ctx.fillRect(x, y, size, size);
                ctx.strokeRect(x, y, size, size);
            });
            ctx.restore();
        };

        owner.resize = function(image) {
            if (images.indexOf(image) !== -1) select(image, true);
        };
        owner.update = function(image, field, first, second) {
            var index = images.indexOf(image);
            if (index === -1) return;
            snapshot();
            if (field === "delete") {
                images.splice(index, 1);
                select(null, false);
            } else if (field === "locked") {
                image.locked = first === 1;
                select(image, false);
            } else if (field === "grid") {
                image.rows = first;
                image.columns = second;
            } else if (field === "opacity") {
                image.opacity = first / 100;
            } else {
                image[field] = first % 360;
            }
            changed();
        };

        function screenPoint(event) {
            var rect = canvas.getBoundingClientRect();
            return [event.clientX - rect.left, event.clientY - rect.top];
        }
        function worldPoint(point) {
            return [(point[0] - view.x) / view.scale, (point[1] - view.y) / view.scale];
        }
        function hit(screen) {
            if (!sim.@com.lushprojects.circuitjs1.client.CirSim::canEditCanvasImage(II)(screen[0], screen[1]))
                return null;
            var point = worldPoint(screen), i;
            if (selected && resizing) {
                var m = matrix(selected);
                for (i = 0; i < corners.length; i++) {
                    var corner = corners[i];
                    var p = project(m, corner[0] * selected.w / 2, corner[1] * selected.h / 2);
                    if (Math.hypot(point[0] - selected.x - p[0], point[1] - selected.y - p[1]) *
                        view.scale <= 16) return {image: selected, corner: corner};
                }
            }
            if (sim.@com.lushprojects.circuitjs1.client.CirSim::canvasComponentAt(II)(screen[0], screen[1]))
                return null;
            for (i = images.length - 1; i >= 0; i--) {
                var image = images[i];
                var local = unproject(matrix(image), point[0] - image.x, point[1] - image.y);
                if (Math.abs(local[0]) <= image.w / 2 && Math.abs(local[1]) <= image.h / 2)
                    return {image: image};
            }
            return null;
        }
        function begin(event, longPress) {
            var screen = screenPoint(event), target = hit(screen);
            if (!target) {
                select(null, false);
                return false;
            }
            var image = target.image;
            if (image.locked && !longPress) return false;
            select(image, image === selected && resizing);
            gesture = {image: image, corner: target.corner, start: screen,
                x: image.x, y: image.y, w: image.w, h: image.h, id: event.identifier};
            if (longPress) {
                var active = gesture;
                active.timer = $wnd.setTimeout(function() {
                    if (gesture !== active) return;
                    active.held = true;
                    owner.@com.lushprojects.circuitjs1.client.CanvasImages::options(Lcom/google/gwt/core/client/JavaScriptObject;II)(
                        image, event.clientX, event.clientY);
                }, 520);
            }
            return true;
        }
        function move(event) {
            if (!gesture || gesture.held) return;
            var screen = screenPoint(event);
            var dx = screen[0] - gesture.start[0], dy = screen[1] - gesture.start[1];
            if (!gesture.moving) {
                if (Math.hypot(dx, dy) < 5) return;
                $wnd.clearTimeout(gesture.timer);
                if (gesture.image.locked) return;
                snapshot();
                gesture.moving = true;
            }
            var image = gesture.image;
            dx /= view.scale;
            dy /= view.scale;
            if (gesture.corner) {
                var m = matrix(image), delta = unproject(m, dx, dy), corner = gesture.corner;
                image.w = Math.max(8, gesture.w + corner[0] * delta[0]);
                image.h = Math.max(8, gesture.h + corner[1] * delta[1]);
                var shift = project(m, corner[0] * (image.w - gesture.w) / 2,
                    corner[1] * (image.h - gesture.h) / 2);
                image.x = gesture.x + shift[0];
                image.y = gesture.y + shift[1];
            } else {
                image.x = gesture.x + dx;
                image.y = gesture.y + dy;
            }
            repaint();
        }
        function finish() {
            if (!gesture) return;
            $wnd.clearTimeout(gesture.timer);
            var moving = gesture.moving;
            gesture = null;
            if (moving) changed();
            repaint();
        }
        function consume(event) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
        var capture = {capture: true, passive: false};
        canvas.addEventListener("touchstart", function(event) {
            if (!touchOwned && event.touches.length === 1) {
                suppressClick = false;
                if (!gesture) touchOwned = begin(event.changedTouches[0], true);
            }
            if (!touchOwned) return;
            if (event.touches.length > 1) {
                // Let the schematic's pinch handler take over the gesture.
                finish();
                touchOwned = false;
                return;
            }
            consume(event);
        }, capture);
        canvas.addEventListener("touchmove", function(event) {
            if (!touchOwned) return;
            for (var i = 0; gesture && i < event.changedTouches.length; i++) {
                var touch = event.changedTouches[i];
                if (touch.identifier === gesture.id) move(touch);
            }
            consume(event);
        }, capture);
        function finishTouch(event) {
            if (!touchOwned) return;
            finish();
            if (!event.touches.length) touchOwned = false;
            consume(event);
        }
        canvas.addEventListener("touchend", finishTouch, capture);
        canvas.addEventListener("touchcancel", finishTouch, capture);
        canvas.addEventListener("pointerdown", function(event) {
            if (event.pointerType === "touch" || event.button !== 0) return;
            suppressClick = false;
            if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
            suppressClick = begin(event, false);
            if (!suppressClick) return;
            canvas.setPointerCapture(event.pointerId);
            consume(event);
        }, capture);
        canvas.addEventListener("pointermove", function(event) {
            if (event.pointerType === "touch" || !gesture || touchOwned) return;
            move(event);
            consume(event);
        }, capture);
        function finishPointer(event) {
            if (event.pointerType !== "touch" && !touchOwned) finish();
        }
        canvas.addEventListener("pointerup", finishPointer, capture);
        canvas.addEventListener("pointercancel", finishPointer, capture);
        canvas.addEventListener("lostpointercapture", finishPointer, capture);
        canvas.addEventListener("mousemove", function(event) {
            if (gesture && !touchOwned) consume(event);
        }, capture);
        canvas.addEventListener("mouseup", function(event) {
            if (event.button === 0 && suppressClick) consume(event);
        }, capture);
        canvas.addEventListener("click", function(event) {
            if (suppressClick) consume(event);
            suppressClick = false;
        }, capture);
        canvas.addEventListener("dblclick", function(event) {
            if (hit(screenPoint(event))) consume(event);
        }, capture);
        canvas.addEventListener("contextmenu", function(event) {
            var target = gesture && touchOwned ? gesture : hit(screenPoint(event));
            if (!target) return;
            consume(event);
            if (touchOwned) return;
            select(target.image, selected === target.image && resizing);
            owner.@com.lushprojects.circuitjs1.client.CanvasImages::options(Lcom/google/gwt/core/client/JavaScriptObject;II)(
                target.image, event.clientX, event.clientY);
        }, capture);
    }-*/;

    native void choose() /*-{ this.choose(); }-*/;
    void clear() {
        if (menuPopup != null)
            menuPopup.hide();
        if (settingsDialog != null)
            settingsDialog.closeDialog();
        clearImages();
    }

    private native void clearImages() /*-{ this.clear(); }-*/;
    native String dump() /*-{ return this.dump(); }-*/;
    native boolean restore(String line) /*-{ return this.restore(line); }-*/;
    native boolean hasImages() /*-{ return this.hasImages(); }-*/;
    native Rectangle bounds() /*-{ return this.bounds(); }-*/;

    native void draw(Context2d context, double scale, double x, double y, int top,
            int width, int height, boolean interactive) /*-{
        this.draw(context, scale, x, y, top, width, height, interactive);
    }-*/;

    native void drawHandles(Context2d context) /*-{ this.drawHandles(context); }-*/;
}
