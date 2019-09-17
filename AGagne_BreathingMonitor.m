%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Project           : Breathing monitor for real time breathing pattern detection – BG06
%
% Program name      : AGagne_BreathingMonitor.m
%
% Author            : Alexandre Gagné - 500593310
%
% Date created      : March 25, 2019
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% ToDo:
% Adjust countPeaks function to have minimum peak width (Done)
% Remove lag period on consecutive ThresholdAlgo 
% Classify more patterns - Kussmaul, Cheyn-stokes
% GUI
% Send Start/Stop bit to Arduino (Done)

%% Initialize
% Bluetooth connection
bt = Bluetooth('AGagne_ESP32', 1);
fopen(bt);

% initialize graph
realtime = figure;
peak = figure;
movegui(realtime,'northwest');
movegui(peak,'northeast');
figure(realtime);
zaxis = animatedline();
xlabel('Time - s');
j = 1;
numpoints = 6000; % number of points read, test purposes only
ymin = 1.4;
ymax = 2;
viewport = 1000; % size of window that is shown in graph

% initialize filter
lag_smooth = 50; % size of trailing window for moving average filter
buffer = nan(1,lag_smooth);

% initialize analysis
fs = 100; % sampling frequency 
bpm_buffer = 2000; % amount of data analyzed at a time
y = zeros(2,bpm_buffer);
m = 1;
n = 1;

% smoothing zscore parameters
lag = 50; % how much your data will be smoothed and how adaptive the 
          % algorithm is to changes in the long-term average of the data
threshold = 2.9; % number of standard deviations from the moving mean 
                 % above which the algorithm will classify a new datapoint 
                 % as being a signal
influence = 0; % influence of signals on the algorithm's detection threshold
pulsewidth = 40; % minimum width of 'signals' that is considered a peak

% matlab 'findpeaks' parameters - width,height,prominence,threshold,distance
peakwidth = 100; % MinPeakWidth
peakprominence = 0.02; % MinPeakProminence
peakdistance = 20; % MinPeakDistance

fprintf(bt,'%s','E'); % send start bit to arduino

for k = 1:numpoints
    %% Filter Data
    % Smoothing Filter
    while(isnan(buffer(lag_smooth))) % if buffer is not full, fill it
        buffer = circshift(buffer,1);
        buffer(1) = fscanf(bt, '%f');
    end
    filtered = mean(buffer); % calculate moving average
    buffer = circshift(buffer,1); % shift points
    buffer(1) = fscanf(bt, '%f'); % add datapoint to buffer
    
    %% Display Data
    % set scrolling axis
    if(j>viewport) % moving viewport axis limits
        xmin=(j-viewport)/fs;
        xmax=j/fs;
    else % initial viewport
        xmin=0;
        xmax=viewport/fs;
    end

    % update axis and add data to graph
    axis([xmin xmax ymin ymax]); % update xaxis
    addpoints(zaxis,j/fs,filtered); % add filtered points to graph
    drawnow limitrate % update graph
    
    %% Analysis
    if(m == bpm_buffer) % amount of data to be analyzed
        % Smoothing zscore 
        [signals,avg,dev] = ThresholdingAlgo(y(2,:),lag,threshold,influence); % send y to threshold algo
        [numpeaks] = countPeaks(signals,pulsewidth); % count peaks 
        breathingrate(1,n) = numpeaks*(60/(bpm_buffer/fs)); % bpm of zscore algo, convert s to min
        
        % Matlab findpeaks
        [pks,locs,w,p] = findpeaks(y(2,:),'MinPeakWidth',peakwidth,...
            'MinPeakProminence',peakprominence,'MinPeakDistance',peakdistance); 
        breathingrate(2,n) = length(locs)*(60/(bpm_buffer/fs));
        
        
        % classify pattern
        if(breathingrate(2,n)<1)
            pattern(n)="Not Breathing - Apnea";
        elseif(breathingrate(2,n)>11 && breathingrate(2,n)<21)
            pattern(n)="Normal - Eupnea";
        elseif(breathingrate(2,n)>20)
            pattern(n)="High - Tachypnea";
        elseif(breathingrate(2,n)<12 && breathingrate(2,n)>0)
            pattern(n)="Low - Bradapnea";
        else
            pattern(n)="---";
        end
        
        data{1,n} = y; % test purposes only, store data
        
        % display results
        figure(peak);
        findpeaks(y(2,:),'MinPeakWidth',peakwidth,...
            'MinPeakProminence',peakprominence,'MinPeakDistance',peakdistance);
        fprintf('%d bpm\n',breathingrate(2,n));
        fprintf('%s\n',pattern(n));
        
        figure(realtime);
        n=n+1;
        m=1;
    else
        y(1,m) = j/fs; % time
        y(2,m) = filtered; % add point to buffer to be analyzed
        m=m+1;
    end
    
    j=j+1;
    
end

fprintf(bt,'%s','Q'); % send stop bit to arduino

[time,breath] = getpoints(zaxis); % test purposes, store data

fclose(bt);
clear('bt');

function [pause] = breathPause(locs)

if length(locs) >= 2
    for k = 1 : length(locs)-1
        dist = locs(k+1)-locs(k);
        if dist > 7000
            pause = true;
        else
            pause = false;
        end
    end
    
end

end

function [signals,avgFilter,stdFilter] = ThresholdingAlgo(y,lag,threshold,influence)
% Initialise signal results
signals = zeros(length(y),1);
% Initialise filtered series
filteredY = y(1:lag+1);
% Initialise filters
avgFilter(lag+1,1) = mean(y(1:lag+1));
stdFilter(lag+1,1) = std(y(1:lag+1));
% Loop over all datapoints y(lag+2),...,y(t)
for i=lag+2:length(y)
    % If new value is a specified number of deviations away
    if abs(y(i)-avgFilter(i-1)) > threshold*stdFilter(i-1)
        if y(i) > avgFilter(i-1)
            % Positive signal
            signals(i) = 1;
        else
            % Negative signal
            signals(i) = -1;
        end
        % Make influence lower
        filteredY(i) = influence*y(i)+(1-influence)*filteredY(i-1);
    else
        % No signal
        signals(i) = 0;
        filteredY(i) = y(i);
    end
    % Adjust the filters
    avgFilter(i) = mean(filteredY(i-lag:i));
    stdFilter(i) = std(filteredY(i-lag:i));
end
% Done, now return results
end

function [peaks] = countPeaks(signals,pulsewidth)
% Count falling edges in signals array
% Peak must have minimum width
peaks = 0;
range = ones(pulsewidth,1); 
for i = 1:(length(signals)-pulsewidth-1)
    if isequal(range,signals(i:(i+pulsewidth-1))) && ...
            (signals(i+pulsewidth)==0||signals(i+pulsewidth)==-1)
        peaks = peaks + 1;
    end
end

end