using System.Globalization;
using Directory = System.IO.Directory;
using MetadataExtractor;
using MetadataDirectory = MetadataExtractor.Directory;



//if (args.Length != 2) {
//	throw new InvalidOperationException("Expected 2 parameters");
//}

//Console.WriteLine(args[0]);
//Console.WriteLine(args[1]);



RenameImages("C:\\Users\\Andrew\\Downloads\\A", "C:\\Users\\Andrew\\Downloads\\B");
return;


static void RenameImages(string sourceDirectory, string outputDirectory) {

	if (!Directory.Exists(sourceDirectory)) {
		throw new InvalidOperationException($"Directory '{sourceDirectory}' does not exist.");
	}

	if (!Directory.Exists(outputDirectory)) {
		Directory.CreateDirectory(outputDirectory);
	}

	string[] sourceFilePaths = Directory.GetFiles(sourceDirectory);

	foreach (string sourceFilePath in sourceFilePaths) {

		string outputFileName = NewFileName(sourceFilePath);
		string outputFilePath = Path.Combine(outputDirectory, outputFileName);

		File.Copy(sourceFilePath, outputFilePath);
	}

	foreach (string sourceSubdirectory in Directory.GetDirectories(sourceDirectory)) {

		string subdirectoryName = new DirectoryInfo(sourceSubdirectory).Name;
		string outputSubdirectory = Path.Combine(outputDirectory, subdirectoryName);
		RenameImages(sourceSubdirectory, outputSubdirectory);
	}


}

static string NewFileName(string filePath) {

	IEnumerable<MetadataDirectory> directories = ImageMetadataReader.ReadMetadata(filePath);

	MetadataDirectory? imageFileDirectoryZero = directories.FirstOrDefault(x => x.Name == "Exif IFD0");
	MetadataDirectory? subImageFileDirectory = directories.FirstOrDefault(x => x.Name == "Exif SubIFD");

	string? topDateTime = imageFileDirectoryZero?.Tags.FirstOrDefault(x => x.Name =="Date/Time")?.Description;
	string? dateTimeString = subImageFileDirectory?.Tags.FirstOrDefault(x => x.Name == "Date/Time Original")?.Description;
	string? subSecond = subImageFileDirectory?.Tags.FirstOrDefault(x => x.Name == "Sub-Sec Time Original")?.Description;
	string? timeZone = subImageFileDirectory?.Tags.FirstOrDefault(x => x.Name == "Time Zone Original")?.Description;


	if (dateTimeString is not null) {

		if (timeZone is null) {
			throw new InvalidOperationException($"Missing timezone in '{filePath}'.");
		}

		if (subSecond is null) {
			throw new InvalidOperationException($"Missing subsecond in '{filePath}'.");
		}

		string combinedDateTime = $"{dateTimeString}.{subSecond} {timeZone}";
		bool success = DateTime.TryParseExact(combinedDateTime, "yyyy:MM:dd HH:mm:ss.fff zzz",
			CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out DateTime dateTime);

		if (!success) {
			throw new InvalidOperationException($"Could not parse '{combinedDateTime}' as a DateTime in '{filePath}'.");
		}

		return dateTime.ToUniversalTime().ToString("yyyy-MM-dd HH_mm_ss.fff") + Path.GetExtension(filePath);

	} else {
		throw new NotImplementedException();
	}

	foreach (MetadataDirectory directory in directories) {

		foreach (Tag tag in directory.Tags) {
			Console.WriteLine($"{directory.Name} - {tag.Name} = {tag.Description}");
		}

	}

	return "test";
}