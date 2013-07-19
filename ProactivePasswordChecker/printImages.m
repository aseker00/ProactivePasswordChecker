function printImages(type, input_dir_path, output_dir_path)
    threshold = getThreshold(input_dir_path, type);
    files = dir(strcat(input_dir_path, strcat(type, '.*.out')));
    for i = 1:length(files)
        img_title = files(i).name(1:length(files(i).name)-4);
        scores = load(strcat(input_dir_path, files(i).name));
        printImage(scores, threshold, img_title, output_dir_path);
    end
end