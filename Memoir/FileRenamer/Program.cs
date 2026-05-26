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



void RenameImages(string sourceDirectory, string outputDirectory) {

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

		Console.WriteLine($"outputFileName: {outputFileName}");

		//File.Copy(sourceFilePath, outputFilePath);
	}

	foreach (string sourceSubdirectory in Directory.GetDirectories(sourceDirectory)) {

		string outputSubdirectory = Path.Combine(outputDirectory, sourceSubdirectory[sourceDirectory.Length..]);

		Console.WriteLine($"outputSubdirectory: {outputSubdirectory}");

		RenameImages(sourceSubdirectory, outputSubdirectory);
	}


}

string NewFileName(string filePath) {

	IEnumerable<MetadataDirectory> directories = ImageMetadataReader.ReadMetadata(filePath);

	foreach (MetadataDirectory directory in directories) {

		foreach (Tag tag in directory.Tags) {
			Console.WriteLine($"{directory.Name} - {tag.Name} = {tag.Description}");
		}

	}

	return "test";
}