# SensorStack_take_home_exercise
IoT Sensor Data Analyzer

## Requirements

Java 8 or higher

## Compilation
```bash
javac SensorAnalyzer.java
```

## Usage
```bash
java SensorAnalyzer sample_data.csv
```

## Output

Top 10 combinations by highest average value

Top 10 combinations by highest variability (std dev)

Statistics: avg, min, max, std dev

Bar chart saved as chart.png in the folder

## Notes
Reads the file line by line, Keeps record for each site + device + metric combination (non-numeric values in the “value” column are skipped).

