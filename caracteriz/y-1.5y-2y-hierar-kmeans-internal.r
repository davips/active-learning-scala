maxitems=100000
library("foreign")
library("parallel")
library("clValid")

path = "/home/davi/caracteriz/arff-pools2/normalizados"
#path = "/home/davi/caracteriz/arff-pools2/teste"

main = function() {
	files = list.files(path,full.names=TRUE)
	#linhas = 
	mclapply(files, mc.cores=10, function(file) {
#	lapply(files, function(file) {
#		print(file)
                if(grepl('arff$', file) && !file.exists(paste(file,".csv",sep=""))) {
			print(file)
			data = read.arff(file)
			aux = measures(data)
   		        write.csv(t(aux), row.names=FALSE, paste(file, ".csv", sep=""))
			print("ok")
		}
	})
	#aux = do.call("rbind",linhas)
}

#retorna vetor com indices de carac.
measures = function(dataset) {
	nclasses = nlevels(dataset$c);
	dataset$c = NULL;
	dataset <- dataset[sample(nrow(dataset), min(maxitems, nrow(dataset)), replace=FALSE), ]

	res <- clValid(dataset, c(nclasses,1.5*nclasses,2*nclasses), clMethods = c("hierarchical","kmeans"), validation=c("internal"), maxitems, method="ward", metric="euclidean", verbose=FALSE)
	c(res@measures[,,1], res@measures[,,2])
}

main()
