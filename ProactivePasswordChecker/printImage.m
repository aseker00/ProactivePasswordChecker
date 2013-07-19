function printImage(scores, threshold, img_title, img_output_path)
    fh = figure('Visible', 'off');
    h = hist(scores, 100);
    hist(scores, 100);
    top = max(h);
    line([threshold threshold], [0, top]);
    title(img_title);
    saveas(fh, strcat([img_output_path, img_title, '.jpg']), 'jpg');
end

