function t = getThreshold (dir_path, type)
M = load(strcat([dir_path, type, '.bp5.out']));
t = quantile(M, 0.01);
clear M;
end

