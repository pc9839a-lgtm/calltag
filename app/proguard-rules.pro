# CallTag release optimization rules.
# Keep diagnostic metadata while allowing R8 to shrink, optimize and obfuscate app code.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Android manifest components plus Jetpack/Firebase/Billing dependencies are covered by
# generated/consumer rules. Add only narrow reflection-specific keeps here when needed.
# Broad catch-all keep rules are intentionally forbidden because they defeat DEX optimization.
