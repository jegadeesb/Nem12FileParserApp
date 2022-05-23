How to pass the argument (path of the CSV file) and Run the TestHarness file:

1. Right Click into the TestHarness test file and select More Run/Debug --> Modify Configurations.
2. Input the path location of the SimpleNem12.csv (in this codebase it will be src/main/resources/SimpleNem12.csv).
3. Click Apply and Ok.
4. Right Click again in the TestHarness test file to run TestHarness.main().
5. Successful output will be printed in the console

Action items:

Additional Test checks were performed to test out the exceptions

1. File not present in the specified path.
2. Begin Record type not equal to TYPE_100.
3. End Record type not equal to TYPE_900.
4. NMI value greater than / Less than 10 chars.
5. Meter Read Quality does not match either Active(A) or Estimate(E)
6. EnergyUnit not equal to KWH.
7. Date format not matching yyyyMMdd.


Modifications:
1.getTotalVolume method access modifier set to Public due to code structuring.