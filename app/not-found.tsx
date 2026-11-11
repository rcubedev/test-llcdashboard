import { Button } from "@/components/Button";

function SparkleBig(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 71 112"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <path
        d="M34.0842 5.00024C36.1659 32.3071 39.9734 45.0186 60.8435 53.8204C43.2615 68.9484 37.8959 81.9723 34.0842 111C31.2151 79.9997 26.929 66.1442 9.84351 53.8204C26.7067 45.9754 31.8813 35.3111 34.0842 6.0034"
        stroke="#01995C"
        strokeWidth="10"
        strokeLinecap="round"
      />
    </svg>
  );
}

function SparkleSmall(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 30 66"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <path
        d="M13.7161 18.3907C13.4394 19.5648 13.1153 20.6738 12.734 21.7208C11.1273 26.132 8.57063 29.3408 4.87167 31.7859C8.86388 36.2403 11.6044 40.4724 13.5456 45.2267C14.0249 46.4006 14.4503 47.5998 14.8349 48.8317C15.2256 47.5092 15.6595 46.2165 16.1515 44.9427C18.077 39.9564 20.7891 35.3763 24.6222 30.3476C20.5888 28.3807 17.5663 25.6161 15.2652 21.5672C14.7041 20.5797 14.1914 19.5215 13.7161 18.3907Z"
        fill="#01995C"
        stroke="#01995C"
        strokeWidth="6"
      />
    </svg>
  );
}

export default function NotFound() {
  return (
    <main className="relative min-h-dvh overflow-hidden">
      <div className="mx-auto relative z-10 flex min-h-dvh max-w-7xl flex-col items-center justify-center px-6 text-center">
        <div className="relative">
          <h1 className="max-w-5xl text-[3.5rem] md:text-[clamp(3.75rem,6rem,10rem)] font-display leading-[0.8] tracking-[-0.06em] md:tracking-[-0.08em]">
            Not found
          </h1>
          
          <SparkleBig className="pointer-events-none absolute hidden -top-7 -right-28 h-28 w-[4.5rem] md:block" />
          <SparkleSmall className="pointer-events-none absolute hidden -top-16 -right-16 h-16 w-16 md:block" />
        </div>

        <p className="mt-14 max-w-xl text-xl font-body">
          The page you&rsquo;re looking for was moved or never made it to the
          final edit.
        </p>

        <div className="mt-8 flex flex-wrap justify-center gap-4 text-lg">
          <Button href="/">BACK TO HOME {">"}</Button>
        </div>
      </div>
    </main>
  );
}